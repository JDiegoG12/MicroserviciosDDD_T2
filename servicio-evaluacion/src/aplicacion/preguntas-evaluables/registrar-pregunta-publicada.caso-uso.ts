import { crearContenidoDePregunta } from '../../dominio/preguntas-evaluables/contenido-de-pregunta';
import { PreguntaEvaluable } from '../../dominio/preguntas-evaluables/pregunta-evaluable';
import { PreguntaEvaluableRepositorio } from '../../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { CasoUso } from '../puertos/entrada/caso-uso';
import { RegistroEventosProcesadosPuerto } from '../puertos/salida/registro-eventos-procesados.puerto';
import { RelojPuerto } from '../puertos/salida/reloj.puerto';
import { RegistrarPreguntaPublicadaComando } from './dto/registrar-pregunta-publicada.comando';
import { ResultadoRegistroEvento } from './dto/resultado-registro-evento';

/**
 * Procesa el evento `PreguntaPublicada` (CONTRATOS.md 7.4): crea o
 * completa la copia local de la pregunta.
 *
 * Disparador: el consumidor de RabbitMQ de la cola `evaluacion.preguntas`
 * para la routing key `pregunta.publicada` (CONTRATOS.md 7.1 y 7.4).
 *
 * Idempotente por `idEvento` con "reclamar y liberar si falla"
 * (CONTRATOS.md 7.7.2): reclama el evento antes de trabajar: si ya estaba
 * reclamado, es un duplicado y no hace nada. Si el guardado falla, libera
 * el reclamo para que una entrega futura del mismo `idEvento` lo pueda
 * reprocesar sin perder datos, y relanza el error original (lo atrapa el
 * consumidor para decidir el `nack`).
 */
export class RegistrarPreguntaPublicadaCasoUso
  implements CasoUso<RegistrarPreguntaPublicadaComando, ResultadoRegistroEvento>
{
  constructor(
    private readonly preguntaEvaluableRepositorio: PreguntaEvaluableRepositorio,
    private readonly registroEventosProcesados: RegistroEventosProcesadosPuerto,
    private readonly reloj: RelojPuerto,
  ) {}

  public async ejecutar(comando: RegistrarPreguntaPublicadaComando): Promise<ResultadoRegistroEvento> {
    const reclamado = await this.registroEventosProcesados.reclamar(comando.idEvento);
    if (!reclamado) {
      return 'DUPLICADO';
    }

    try {
      const contenido = crearContenidoDePregunta({
        contexto: comando.contexto,
        preguntaDirecta: comando.preguntaDirecta,
        opciones: comando.opciones,
        letraCorrecta: comando.letraCorrecta,
        clasificacion: comando.clasificacion,
        nivelDificultad: comando.nivelDificultad,
        fechaPublicacion: comando.fechaPublicacion,
      });

      const ahora = this.reloj.ahora();
      const existente = await this.preguntaEvaluableRepositorio.obtenerPorId(comando.preguntaId);
      const preguntaEvaluable = existente
        ? existente.incorporarPublicacion(contenido, ahora)
        : PreguntaEvaluable.desdePublicacion(comando.preguntaId, contenido, ahora);

      await this.preguntaEvaluableRepositorio.guardar(preguntaEvaluable);

      return 'REGISTRADO';
    } catch (error) {
      await this.registroEventosProcesados.liberar(comando.idEvento);
      throw error;
    }
  }
}
