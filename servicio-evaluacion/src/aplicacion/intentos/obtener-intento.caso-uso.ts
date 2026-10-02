import { IntentoDeSimulacroRepositorio } from '../../dominio/intentos/intento-de-simulacro.repositorio';
import { IntentoId } from '../../dominio/intentos/intento-id';
import { PreguntaEvaluableRepositorio } from '../../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { IntentoNoEncontradoExcepcion } from '../compartido/excepciones/intento-no-encontrado.excepcion';
import { Rol } from '../compartido/usuario-actual';
import { CasoUso } from '../puertos/entrada/caso-uso';
import { RelojPuerto } from '../puertos/salida/reloj.puerto';
import { armarIntentoRespuesta } from './armador-intento-respuesta';
import { CierreDeIntento } from './cierre-de-intento';
import { ObtenerIntentoConsulta } from './dto/obtener-intento.consulta';
import { IntentoRespuesta } from './dto/intento-respuesta';

/**
 * Obtiene un IntentoDeSimulacro por su identificador.
 *
 * Endpoint: `GET /intentos/{intentoId}` (CONTRATOS.md 8.3): el `ESTUDIANTE`
 * solo consulta el suyo; el `DOCENTE` puede leer cualquiera, en solo
 * lectura (MODELO-DOMINIO.md A.2).
 */
export class ObtenerIntentoCasoUso implements CasoUso<ObtenerIntentoConsulta, IntentoRespuesta> {
  constructor(
    private readonly intentoDeSimulacroRepositorio: IntentoDeSimulacroRepositorio,
    private readonly preguntaEvaluableRepositorio: PreguntaEvaluableRepositorio,
    private readonly cierreDeIntento: CierreDeIntento,
    private readonly reloj: RelojPuerto,
  ) {}

  public async ejecutar(consulta: ObtenerIntentoConsulta): Promise<IntentoRespuesta> {
    consulta.usuario.exigirAlgunRol(Rol.ESTUDIANTE, Rol.DOCENTE);

    const intentoId = IntentoId.desde(consulta.intentoId);
    const intento = await this.intentoDeSimulacroRepositorio.obtenerPorId(intentoId);
    if (!intento) {
      throw new IntentoNoEncontradoExcepcion(consulta.intentoId);
    }
    if (!consulta.usuario.tieneRol(Rol.DOCENTE)) {
      intento.verificarPropietario(consulta.usuario.id);
    }

    const ahora = this.reloj.ahora();
    await this.cierreDeIntento.ponerAlDia(intento, ahora);

    const preguntas = await this.preguntaEvaluableRepositorio.obtenerPorIds(intento.preguntaIds());
    return armarIntentoRespuesta(intento, preguntas);
  }
}
