import { Injectable } from '@nestjs/common';
import { ChannelWrapper } from 'amqp-connection-manager';
import { ConfirmChannel } from 'amqplib';
import { PublicadorEventosPuerto } from '../../aplicacion/puertos/salida/publicador-eventos.puerto';
import { aTextoIso } from '../../aplicacion/compartido/formato-fecha';
import { EventoDeDominio } from '../../dominio/compartido/evento-de-dominio';
import { IntentoDeSimulacroCalificadoEvento } from '../../dominio/intentos/eventos/intento-de-simulacro-calificado.evento';
import { ConfiguracionServicio } from '../configuracion/configuracion.servicio';
import { obtenerIdCorrelacionActual } from '../correlacion/contexto-correlacion';
import { registrar } from '../observabilidad/registrador';
import { ConexionRabbitMqServicio } from './conexion-rabbitmq.servicio';
import { construirSobreEvento } from './sobre-evento';
import { asegurarTopologiaRabbitMq, EXCHANGE_EVALUACION_EVENTOS, ROUTING_KEY_INTENTO_CALIFICADO } from './topologia-rabbitmq';

const TIPO_EVENTO_CALIFICADO = 'IntentoDeSimulacroCalificado';

/**
 * Implementacion de `PublicadorEventosPuerto` con RabbitMQ (CONTRATOS.md
 * 7.6).
 *
 * Solo publica `IntentoDeSimulacroCalificado`: los demas eventos de este
 * servicio (`SimulacroDefinido`, `IntentoDeSimulacroIniciado`,
 * `IntentoDeSimulacroFinalizado`) son internos y solo se registran en el
 * log (CONTRATOS.md 7.6: "hoy no tiene consumidor" se refiere al evento
 * publicado; los demas ni siquiera viajan por el broker).
 *
 * Usa un canal de confirmacion (`amqp-connection-manager` crea
 * `ConfirmChannel` por defecto): `publish()` no resuelve hasta que el
 * broker confirma el mensaje (CONTRATOS.md 7.7.1, "publisher confirms").
 */
@Injectable()
export class PublicadorEventosRabbitMqAdaptador implements PublicadorEventosPuerto {
  private readonly canal: ChannelWrapper;

  constructor(
    conexion: ConexionRabbitMqServicio,
    private readonly configuracion: ConfiguracionServicio,
  ) {
    this.canal = conexion.crearCanal({
      name: 'publicador-eventos',
      json: true,
      setup: (canalCrudo: ConfirmChannel) => asegurarTopologiaRabbitMq(canalCrudo),
    });
  }

  public async publicar(eventos: readonly EventoDeDominio[]): Promise<void> {
    for (const evento of eventos) {
      if (evento.tipoEvento !== TIPO_EVENTO_CALIFICADO) {
        registrar('info', `Evento interno ${evento.tipoEvento}: no se publica por RabbitMQ (CONTRATOS.md 7.6).`);
        continue;
      }
      await this.publicarIntentoCalificado(evento as IntentoDeSimulacroCalificadoEvento);
    }
  }

  private async publicarIntentoCalificado(evento: IntentoDeSimulacroCalificadoEvento): Promise<void> {
    const datos = {
      intentoId: evento.intentoId,
      simulacroId: evento.simulacroId,
      estudianteId: evento.estudianteId,
      fechaInicio: aTextoIso(evento.fechaInicio),
      fechaFinalizacion: aTextoIso(evento.fechaFinalizacion),
      finalizadoPor: evento.finalizadoPor,
      calificacion: {
        totalPreguntas: evento.calificacion.totalPreguntas,
        correctas: evento.calificacion.correctas,
        puntaje: evento.calificacion.puntaje,
      },
      desglosePorCompetencia: evento.calificacion.desglosePorCompetencia.map((item) => ({ ...item })),
    };

    const sobre = construirSobreEvento(
      TIPO_EVENTO_CALIFICADO,
      aTextoIso(evento.fechaOcurrencia),
      this.configuracion.nombreServicio,
      obtenerIdCorrelacionActual(),
      datos,
    );

    // CONTRATOS.md 7.2: propiedades AMQP exactas de cada mensaje.
    await this.canal.publish(EXCHANGE_EVALUACION_EVENTOS, ROUTING_KEY_INTENTO_CALIFICADO, sobre, {
      contentType: 'application/json',
      contentEncoding: 'utf-8',
      persistent: true,
      messageId: sobre.idEvento,
      type: sobre.tipoEvento,
      timestamp: Math.floor(Date.now() / 1000),
    });

    registrar('info', 'Evento IntentoDeSimulacroCalificado publicado.', {
      idEvento: sobre.idEvento,
      intentoId: evento.intentoId,
    });
  }
}
