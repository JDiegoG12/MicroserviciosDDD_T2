import { EventEmitter } from 'node:events';

/**
 * Doble de la `Connection` de Mongoose, para probar
 * `PreguntaEditorialConsumidor` sin levantar MongoDB real. Expone
 * `readyState` y los eventos `connected`/`disconnected` que el consumidor
 * escucha, y metodos para simularlos desde la prueba.
 */
export class ConexionMongoFalsa extends EventEmitter {
  public readyState: number;

  constructor(conectadaDeEntrada = true) {
    super();
    this.readyState = conectadaDeEntrada ? 1 : 0;
  }

  /** Simula que Mongo se conecta (o reconecta). */
  public simularConexion(): void {
    this.readyState = 1;
    this.emit('connected');
  }

  /** Simula que Mongo se desconecta. */
  public simularDesconexion(): void {
    this.readyState = 0;
    this.emit('disconnected');
  }
}
