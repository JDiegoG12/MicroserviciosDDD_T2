import { EstadoIntento } from '../../dominio/intentos/estado-intento';
import { IntentoDeSimulacroRepositorio } from '../../dominio/intentos/intento-de-simulacro.repositorio';
import { IntentoId } from '../../dominio/intentos/intento-id';
import { PreguntaEvaluableRepositorio } from '../../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { IntentoFinalizadoExcepcion } from '../../dominio/excepciones/intento-finalizado.excepcion';
import { IntentoNoEncontradoExcepcion } from '../compartido/excepciones/intento-no-encontrado.excepcion';
import { Rol } from '../compartido/usuario-actual';
import { CasoUso } from '../puertos/entrada/caso-uso';
import { RelojPuerto } from '../puertos/salida/reloj.puerto';
import { armarIntentoRespuesta } from './armador-intento-respuesta';
import { CierreDeIntento } from './cierre-de-intento';
import { FinalizarIntentoComando } from './dto/finalizar-intento.comando';
import { IntentoRespuesta } from './dto/intento-respuesta';

/**
 * CU-14 (cierre) + CU-15 (calificacion): finaliza el intento y lo califica
 * en el mismo caso de uso (MODELO-DOMINIO.md A.3: "Se ejecuta en el mismo
 * caso de uso de finalizacion, dentro del proceso; no pasa por el
 * broker").
 *
 * Endpoint: `POST /intentos/{intentoId}/finalizacion` (CONTRATOS.md 8.3),
 * rol `ESTUDIANTE` (dueno del intento).
 *
 * CONTRATOS.md 8.3: "Finalizar un intento ya vencido: si al llamar el
 * intento seguia EN_CURSO pero ya habia vencido, se finaliza con
 * TIEMPO_AGOTADO, se califica y se responde 200 (no es error). Solo
 * responde 409 INTENTO_FINALIZADO si ya estaba FINALIZADO o CALIFICADO
 * antes de la peticion."
 */
export class FinalizarIntentoCasoUso implements CasoUso<FinalizarIntentoComando, IntentoRespuesta> {
  constructor(
    private readonly intentoDeSimulacroRepositorio: IntentoDeSimulacroRepositorio,
    private readonly preguntaEvaluableRepositorio: PreguntaEvaluableRepositorio,
    private readonly cierreDeIntento: CierreDeIntento,
    private readonly reloj: RelojPuerto,
  ) {}

  public async ejecutar(comando: FinalizarIntentoComando): Promise<IntentoRespuesta> {
    comando.usuario.exigirAlgunRol(Rol.ESTUDIANTE);

    const intentoId = IntentoId.desde(comando.intentoId);
    const intento = await this.intentoDeSimulacroRepositorio.obtenerPorId(intentoId);
    if (!intento) {
      throw new IntentoNoEncontradoExcepcion(comando.intentoId);
    }
    intento.verificarPropietario(comando.usuario.id);

    if (intento.estado !== EstadoIntento.EN_CURSO) {
      throw new IntentoFinalizadoExcepcion('El intento ya estaba finalizado antes de esta peticion.');
    }

    const ahora = this.reloj.ahora();
    await this.cierreDeIntento.ponerAlDia(intento, ahora);

    if (intento.estado === EstadoIntento.EN_CURSO) {
      intento.finalizar('ESTUDIANTE', ahora);
      await this.cierreDeIntento.calificarGuardarYPublicar(intento);
    }

    const preguntas = await this.preguntaEvaluableRepositorio.obtenerPorIds(intento.preguntaIds());
    return armarIntentoRespuesta(intento, preguntas);
  }
}
