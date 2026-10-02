import { IntentoDeSimulacroRepositorio } from '../../dominio/intentos/intento-de-simulacro.repositorio';
import { IntentoId } from '../../dominio/intentos/intento-id';
import { PreguntaEvaluableRepositorio } from '../../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { IntentoNoEncontradoExcepcion } from '../compartido/excepciones/intento-no-encontrado.excepcion';
import { Rol } from '../compartido/usuario-actual';
import { CasoUso } from '../puertos/entrada/caso-uso';
import { PublicadorEventosPuerto } from '../puertos/salida/publicador-eventos.puerto';
import { RelojPuerto } from '../puertos/salida/reloj.puerto';
import { armarIntentoRespuesta } from './armador-intento-respuesta';
import { CierreDeIntento } from './cierre-de-intento';
import { RegistrarRespuestaComando } from './dto/registrar-respuesta.comando';
import { IntentoRespuesta } from './dto/intento-respuesta';

/**
 * CU-14 (respuesta): Presentar simulacro, registrar una respuesta.
 *
 * Endpoint futuro: `PUT /intentos/{intentoId}/respuestas/{preguntaId}`
 * (CONTRATOS.md 8.3), rol `ESTUDIANTE` (dueno del intento).
 *
 * Aplica primero el vencimiento perezoso (a traves de `CierreDeIntento`):
 * si el intento ya vencio, la respuesta se rechaza con
 * `INTENTO_FINALIZADO` y el intento queda calificado con
 * `TIEMPO_AGOTADO`.
 */
export class RegistrarRespuestaCasoUso implements CasoUso<RegistrarRespuestaComando, IntentoRespuesta> {
  constructor(
    private readonly intentoDeSimulacroRepositorio: IntentoDeSimulacroRepositorio,
    private readonly preguntaEvaluableRepositorio: PreguntaEvaluableRepositorio,
    private readonly cierreDeIntento: CierreDeIntento,
    private readonly publicadorEventos: PublicadorEventosPuerto,
    private readonly reloj: RelojPuerto,
  ) {}

  /**
   * Registra la respuesta de un estudiante a una pregunta del intento.
   *
   * Antes de registrar aplica el vencimiento perezoso (`CierreDeIntento`):
   * si el intento ya vencio, esta llamada lo cierra y lo califica con
   * `TIEMPO_AGOTADO` y rechaza la respuesta con `INTENTO_FINALIZADO`
   * (INV-31).
   *
   * @param comando Usuario que responde, `intentoId`, `preguntaId` y la
   * letra seleccionada.
   * @returns El intento actualizado, con la respuesta ya registrada.
   * @throws IntentoNoEncontradoExcepcion Si `intentoId` no existe.
   * @throws IntentoFinalizadoExcepcion Si el intento ya no esta EN_CURSO.
   * @throws PreguntaNoPerteneceAlSimulacroExcepcion Si `preguntaId` no es
   * una de las preguntas del simulacro del intento (INV-29).
   */
  public async ejecutar(comando: RegistrarRespuestaComando): Promise<IntentoRespuesta> {
    comando.usuario.exigirAlgunRol(Rol.ESTUDIANTE);

    const intentoId = IntentoId.desde(comando.intentoId);
    const intento = await this.intentoDeSimulacroRepositorio.obtenerPorId(intentoId);
    if (!intento) {
      throw new IntentoNoEncontradoExcepcion(comando.intentoId);
    }
    intento.verificarPropietario(comando.usuario.id);

    const ahora = this.reloj.ahora();
    await this.cierreDeIntento.ponerAlDia(intento, ahora);

    // Si `ponerAlDia` acaba de cerrar y calificar el intento por vencimiento,
    // este metodo lanza INTENTO_FINALIZADO (INV-31), tal como exige
    // CONTRATOS.md 11.3.
    intento.registrarRespuesta(comando.preguntaId, comando.letraSeleccionada, ahora);

    await this.intentoDeSimulacroRepositorio.guardar(intento);
    await this.publicadorEventos.publicar(intento.extraerEventos());

    const preguntas = await this.preguntaEvaluableRepositorio.obtenerPorIds(intento.preguntaIds());
    return armarIntentoRespuesta(intento, preguntas);
  }
}
