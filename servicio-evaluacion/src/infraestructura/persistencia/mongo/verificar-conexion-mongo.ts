import { Connection } from 'mongoose';
import { BaseDeDatosNoDisponibleExcepcion } from './base-de-datos-no-disponible.excepcion';

/** `readyState` de Mongoose cuando la conexion esta activa. */
const CONECTADO = 1;

/**
 * Comprueba que la conexion a MongoDB este activa antes de usarla.
 *
 * Con `lazyConnection: true` y `bufferCommands: false` (CONTRATOS.md 9.3.6),
 * el arranque del servicio no espera a Mongo y las operaciones no se
 * encolan en silencio mientras esta desconectado: cada repositorio llama a
 * esta funcion al principio de sus metodos publicos para fallar rapido con
 * un codigo claro en vez de dejar que una operacion de Mongoose rebote con
 * un error generico.
 *
 * @param conexion Conexion de Mongoose del modelo que se va a usar.
 * @throws BaseDeDatosNoDisponibleExcepcion Si la conexion no esta activa.
 */
export function verificarConexionMongo(conexion: Connection): void {
  if (conexion.readyState !== CONECTADO) {
    throw new BaseDeDatosNoDisponibleExcepcion();
  }
}
