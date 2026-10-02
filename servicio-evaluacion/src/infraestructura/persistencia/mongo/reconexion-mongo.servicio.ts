import { Injectable, OnModuleInit } from '@nestjs/common';
import { InjectConnection } from '@nestjs/mongoose';
import { Connection } from 'mongoose';
import { ConfiguracionServicio, OPCIONES_MONGOOSE } from '../../configuracion/configuracion.servicio';
import { registrar } from '../../observabilidad/registrador';

/** `readyState` de Mongoose cuando la conexion esta activa. */
const MONGO_CONECTADO = 1;

/** Cada cuanto se reintenta mientras la conexion sigue `disconnected`. */
const INTERVALO_REINTENTO_MS = 5_000;

/** `readyState` de Mongoose cuando la conexion esta cerrada del todo. */
const MONGO_DESCONECTADO = 0;

/**
 * Reintenta la conexion a MongoDB mientras el **primer** intento no haya
 * tenido exito (CONTRATOS.md 9.3.6).
 *
 * Mongoose documenta que, a diferencia de una conexion que si se llego a
 * establecer y luego se cae (esa el driver la reconecta solo, con su
 * propio monitoreo de topologia; ver la prueba de resiliencia de este
 * servicio), **una conexion inicial que falla no se reintenta sola**:
 * "Mongoose will not automatically try to reconnect after initial
 * connection failure... by design". Con `lazyConnection: true`
 * (CONTRATOS.md 9.3.6: el proceso no debe bloquearse ni caerse si Mongo
 * no esta lista al arrancar), eso deja la conexion `disconnected` para
 * siempre si nadie la vuelve a abrir.
 *
 * Este servicio llena ese vacio reintentando `Connection.openUri(...)`
 * con las mismas opciones cada `INTERVALO_REINTENTO_MS`, pero **solo**
 * hasta la primera conexion exitosa: a partir de ahi se retira por
 * completo y deja cualquier reconexion futura en manos del driver (que ya
 * sabe hacerlo solo), para no competir con el si llega a caerse despues.
 */
@Injectable()
export class ReconexionMongoServicio implements OnModuleInit {
  private conectadaAlgunaVez = false;
  private reintentando = false;

  constructor(
    @InjectConnection() private readonly conexion: Connection,
    private readonly configuracion: ConfiguracionServicio,
  ) {}

  public onModuleInit(): void {
    this.conexion.once('connected', () => {
      this.conectadaAlgunaVez = true;
    });
    this.conexion.on('error', (error: Error) => {
      registrar('warn', 'Error de conexion con MongoDB.', { error: error.message });
      this.programarReintento();
    });

    if (this.conexion.readyState !== MONGO_CONECTADO) {
      this.programarReintento();
    }
  }

  private programarReintento(): void {
    if (this.reintentando || this.conectadaAlgunaVez) {
      return;
    }
    this.reintentando = true;
    setTimeout(() => void this.reintentar(), INTERVALO_REINTENTO_MS).unref();
  }

  private async reintentar(): Promise<void> {
    if (this.conectadaAlgunaVez) {
      this.reintentando = false;
      return;
    }
    if (this.conexion.readyState !== MONGO_DESCONECTADO) {
      // El primer intento todavia esta en curso (`connecting`): no hay
      // nada que reabrir; se vuelve a revisar en el siguiente ciclo.
      setTimeout(() => void this.reintentar(), INTERVALO_REINTENTO_MS).unref();
      return;
    }
    try {
      await this.conexion.openUri(this.configuracion.mongoUrl, OPCIONES_MONGOOSE);
      // El listener de 'connected' marca `conectadaAlgunaVez`; nada mas que hacer aqui.
      this.reintentando = false;
    } catch (error) {
      registrar('warn', 'Reintento de conexion con MongoDB fallido; se reintentara.', {
        error: error instanceof Error ? error.message : String(error),
      });
      // Se queda en el mismo ciclo (no vuelve a pasar por
      // `programarReintento`, que ya hizo su trabajo): programa el
      // siguiente intento directamente.
      setTimeout(() => void this.reintentar(), INTERVALO_REINTENTO_MS).unref();
    }
  }
}
