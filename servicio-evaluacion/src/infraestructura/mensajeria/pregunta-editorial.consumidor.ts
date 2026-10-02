import { Inject, Injectable, OnModuleInit } from '@nestjs/common';
import { InjectConnection } from '@nestjs/mongoose';
import { ChannelWrapper } from 'amqp-connection-manager';
import { ConfirmChannel, ConsumeMessage } from 'amqplib';
import { Connection } from 'mongoose';
import { RegistrarPreguntaArchivadaCasoUso } from '../../aplicacion/preguntas-evaluables/registrar-pregunta-archivada.caso-uso';
import { RegistrarPreguntaPublicadaCasoUso } from '../../aplicacion/preguntas-evaluables/registrar-pregunta-publicada.caso-uso';
import { generarUuid } from '../../dominio/compartido/uuid';
import { ejecutarConCorrelacion } from '../correlacion/contexto-correlacion';
import { registrar } from '../observabilidad/registrador';
import { BaseDeDatosNoDisponibleExcepcion } from '../persistencia/mongo/base-de-datos-no-disponible.excepcion';
import { ConexionRabbitMqServicio } from './conexion-rabbitmq.servicio';
import { MensajeInvalidoExcepcion } from './mensaje-invalido.excepcion';
import { asegurarTopologiaRabbitMq, COLA_EVALUACION_PREGUNTAS, PREFETCH_CONSUMIDOR } from './topologia-rabbitmq';
import {
  interpretarSobre,
  validarMensajePreguntaArchivada,
  validarMensajePreguntaPublicada,
} from './validador-mensaje-pregunta';

/** `readyState` de Mongoose cuando la conexion esta activa. */
const MONGO_CONECTADO = 1;

/**
 * Consumidor de la cola `evaluacion.preguntas` (CONTRATOS.md 7.1, 7.4, 7.5
 * y 7.7). Distribuye cada mensaje a `RegistrarPreguntaPublicadaCasoUso` o
 * `RegistrarPreguntaArchivadaCasoUso` segun su `tipoEvento`.
 *
 * `prefetch` de 10 y **ack manual** despues de que el caso de uso termina
 * de guardar (CONTRATOS.md 7.7.1). Un mensaje invalido se responde con
 * `nack` sin reencolar: por la cola `x-dead-letter-exchange`, termina en
 * `evaluacion.preguntas.dlq` (CONTRATOS.md 7.7.4).
 *
 * CONTRATOS.md 7.7.5: para que un mensaje nunca termine en la DLQ solo
 * porque MongoDB esta caido, este consumidor **solo consume mientras hay
 * conexion con la base de datos**: no empieza a consumir hasta que Mongo
 * conecta (evento `connected` de la `Connection` de Mongoose), y si la
 * conexion se pierde (evento `disconnected`) cancela el consumo y lo
 * reanuda al reconectar. La declaracion de la topologia de RabbitMQ no
 * depende de Mongo: ocurre en el `setup` del canal, antes de saber si
 * Mongo esta lista.
 */
@Injectable()
export class PreguntaEditorialConsumidor implements OnModuleInit {
  private canal!: ChannelWrapper;
  private consumerTag: string | null = null;

  constructor(
    private readonly conexion: ConexionRabbitMqServicio,
    @Inject(RegistrarPreguntaPublicadaCasoUso)
    private readonly registrarPublicada: RegistrarPreguntaPublicadaCasoUso,
    @Inject(RegistrarPreguntaArchivadaCasoUso)
    private readonly registrarArchivada: RegistrarPreguntaArchivadaCasoUso,
    @InjectConnection() private readonly conexionMongo: Connection,
  ) {}

  public onModuleInit(): void {
    this.canal = this.conexion.crearCanal({
      name: 'consumidor-preguntas',
      setup: async (canalCrudo: ConfirmChannel) => {
        // La topologia (exchanges, colas, bindings) no depende de Mongo.
        await asegurarTopologiaRabbitMq(canalCrudo);
        await canalCrudo.prefetch(PREFETCH_CONSUMIDOR);
        registrar('info', `Topologia de ${COLA_EVALUACION_PREGUNTAS} declarada.`);
        // Si el canal AMQP se reconecta mientras Mongo ya estaba lista,
        // amqp-connection-manager vuelve a consumir solo (sigue en su
        // lista interna de consumidores); si Mongo seguia caida, no habia
        // consumidor activo que reconectar.
        if (this.mongoEstaConectada() && !this.consumerTag) {
          await this.reanudarConsumo();
        }
      },
    });

    this.conexionMongo.on('connected', () => void this.alConectarMongo());
    this.conexionMongo.on('disconnected', () => void this.alDesconectarMongo());
    if (this.mongoEstaConectada()) {
      void this.alConectarMongo();
    }
  }

  private mongoEstaConectada(): boolean {
    return this.conexionMongo.readyState === MONGO_CONECTADO;
  }

  private async alConectarMongo(): Promise<void> {
    registrar('info', 'MongoDB conectado: reanudando el consumo de evaluacion.preguntas.');
    await this.reanudarConsumo();
  }

  private async alDesconectarMongo(): Promise<void> {
    registrar('warn', 'MongoDB desconectado: pausando el consumo de evaluacion.preguntas.');
    await this.pausarConsumo();
  }

  private async reanudarConsumo(): Promise<void> {
    if (this.consumerTag) {
      return;
    }
    try {
      const { consumerTag } = await this.canal.consume(COLA_EVALUACION_PREGUNTAS, (mensaje) =>
        this.procesarMensaje(mensaje),
      );
      this.consumerTag = consumerTag;
      registrar('info', `Consumidor escuchando la cola ${COLA_EVALUACION_PREGUNTAS}.`);
    } catch (error) {
      // El canal AMQP aun no esta listo (por ejemplo, se esta reconectando
      // al broker); el proximo `setup` o el proximo evento `connected` de
      // Mongo lo vuelve a intentar.
      registrar('warn', 'No se pudo iniciar el consumo todavia; se reintentara.', {
        error: error instanceof Error ? error.message : String(error),
      });
    }
  }

  private async pausarConsumo(): Promise<void> {
    if (!this.consumerTag) {
      return;
    }
    const etiqueta = this.consumerTag;
    this.consumerTag = null;
    try {
      await this.canal.cancel(etiqueta);
    } catch (error) {
      registrar('warn', 'No se pudo cancelar el consumo (probablemente el canal ya se cerro).', {
        error: error instanceof Error ? error.message : String(error),
      });
    }
  }

  private async procesarMensaje(mensaje: ConsumeMessage | null): Promise<void> {
    if (!mensaje) {
      // El canal se cerro (amqp-connection-manager cancela el consumo); nada que hacer.
      return;
    }

    let idEventoParaLog: string | undefined;
    let tipoEventoParaLog: string | undefined;

    try {
      const sobre = interpretarSobre(mensaje.content);
      idEventoParaLog = sobre.idEvento;
      tipoEventoParaLog = sobre.tipoEvento;

      await ejecutarConCorrelacion(sobre.idCorrelacion ?? generarUuid(), async () => {
        if (sobre.tipoEvento === 'PreguntaPublicada') {
          const comando = validarMensajePreguntaPublicada(sobre.idEvento, sobre.datos);
          const resultado = await this.registrarPublicada.ejecutar(comando);
          registrar('info', 'PreguntaPublicada procesada.', {
            idEvento: sobre.idEvento,
            tipoEvento: sobre.tipoEvento,
            resultado,
          });
        } else {
          const comando = validarMensajePreguntaArchivada(sobre.idEvento, sobre.datos);
          const resultado = await this.registrarArchivada.ejecutar(comando);
          registrar('info', 'PreguntaArchivada procesada.', {
            idEvento: sobre.idEvento,
            tipoEvento: sobre.tipoEvento,
            resultado,
          });
        }
      });

      this.canal.ack(mensaje);
    } catch (error) {
      if (error instanceof BaseDeDatosNoDisponibleExcepcion) {
        // CONTRATOS.md 7.7.5: Mongo se desconecto justo en medio del
        // procesamiento (la pausa normal no alcanzo a evitarlo). Se
        // reencola para no perder el mensaje en la DLQ; se reprocesara
        // cuando el consumo se reanude.
        //
        // DUDA: si el intento de liberar el reclamo de idempotencia
        // (RegistroEventosProcesadosPuerto.liberar) tambien fallo por la
        // misma caida de Mongo, el reclamo queda huerfano y el reenvio de
        // este mensaje se vera (incorrectamente) como un duplicado hasta
        // que alguien lo libere a mano. Es una ventana estrecha (justo el
        // instante en que Mongo cae a mitad de un mensaje) que no se
        // resolvio por completo en esta etapa.
        registrar('warn', 'MongoDB no disponible a mitad del procesamiento: se reencola el mensaje.', {
          idEvento: idEventoParaLog,
          tipoEvento: tipoEventoParaLog,
        });
        this.canal.nack(mensaje, false, true);
        await this.pausarConsumo();
        return;
      }

      if (error instanceof MensajeInvalidoExcepcion) {
        registrar('warn', 'Mensaje invalido: se envia a la DLQ sin reencolar.', {
          idEvento: idEventoParaLog,
          tipoEvento: tipoEventoParaLog,
          error: error.message,
        });
      } else {
        registrar('error', 'Error al procesar el mensaje: se envia a la DLQ sin reencolar.', {
          idEvento: idEventoParaLog,
          tipoEvento: tipoEventoParaLog,
          error: error instanceof Error ? error.message : String(error),
        });
      }
      // CONTRATOS.md 7.7.4: nunca se reencola, para no generar un bucle
      // infinito; el mensaje termina en evaluacion.preguntas.dlq gracias
      // al x-dead-letter-exchange de la cola.
      this.canal.nack(mensaje, false, false);
    }
  }
}
