import { Injectable, OnModuleDestroy } from '@nestjs/common';
import { AmqpConnectionManager, ChannelWrapper, connect, CreateChannelOpts } from 'amqp-connection-manager';
import { ConfiguracionServicio } from '../configuracion/configuracion.servicio';
import { registrar } from '../observabilidad/registrador';

/** Tope maximo de espera entre reintentos de conexion (CONTRATOS.md 9.3.6). */
const ESPERA_MAXIMA_SEGUNDOS = 30;

/**
 * Gestiona la unica conexion AMQP del servicio (con `amqp-connection-manager`,
 * CONTRATOS.md 9.3.6: "El servicio arranca aunque sus dependencias no esten
 * listas y reintenta conectarse").
 *
 * Implementa la espera progresiva ajustando `reconnectTimeInSeconds` del
 * gestor en cada intento fallido (1 s, 2 s, 4 s, 8 s, 16 s, 30 s, 30 s...,
 * hasta un maximo de 30 s entre intentos), y la reinicia a 1 s apenas se
 * reconecta.
 */
@Injectable()
export class ConexionRabbitMqServicio implements OnModuleDestroy {
  private readonly gestor: AmqpConnectionManager;
  private intentosFallidosSeguidos = 0;

  constructor(private readonly configuracion: ConfiguracionServicio) {
    this.gestor = connect([this.configuracion.rabbitMqUrl], {
      heartbeatIntervalInSeconds: 5,
      reconnectTimeInSeconds: 1,
    });

    this.gestor.on('connect', () => {
      this.intentosFallidosSeguidos = 0;
      this.gestor.reconnectTimeInSeconds = 1;
      registrar('info', 'Conexion establecida con RabbitMQ.');
    });
    this.gestor.on('connectFailed', ({ err }) => {
      this.intentosFallidosSeguidos += 1;
      const esperaSegundos = Math.min(ESPERA_MAXIMA_SEGUNDOS, 2 ** this.intentosFallidosSeguidos);
      this.gestor.reconnectTimeInSeconds = esperaSegundos;
      registrar('warn', 'No se pudo conectar a RabbitMQ; se reintentara con espera progresiva.', {
        intento: this.intentosFallidosSeguidos,
        esperaSegundos,
        error: err.message,
      });
    });
    this.gestor.on('disconnect', ({ err }) => {
      registrar('warn', 'Se perdio la conexion con RabbitMQ; se reintentara.', { error: err?.message });
    });
  }

  /**
   * Crea un canal administrado (reconecta solo y vuelve a ejecutar su
   * `setup` en cada reconexion).
   *
   * @param opciones Opciones de `amqp-connection-manager` para el canal.
   * @returns El canal creado.
   */
  public crearCanal(opciones: CreateChannelOpts): ChannelWrapper {
    return this.gestor.createChannel(opciones);
  }

  public async onModuleDestroy(): Promise<void> {
    await this.gestor.close();
  }
}
