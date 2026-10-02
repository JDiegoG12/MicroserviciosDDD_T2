import { PreguntaEvaluable } from '../../dominio/preguntas-evaluables/pregunta-evaluable';
import { PreguntaEvaluableRepositorio } from '../../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { CasoUso } from '../puertos/entrada/caso-uso';
import { RegistroEventosProcesadosPuerto } from '../puertos/salida/registro-eventos-procesados.puerto';
import { RelojPuerto } from '../puertos/salida/reloj.puerto';
import { RegistrarPreguntaArchivadaComando } from './dto/registrar-pregunta-archivada.comando';
import { ResultadoRegistroEvento } from './dto/resultado-registro-evento';

/**
 * Procesa el evento `PreguntaArchivada` (CONTRATOS.md 7.5): archiva la
 * copia local, conservando su contenido si ya lo tenia, o crea una marca
 * de archivo si este servicio aun no conocia la pregunta (orden no
 * garantizado, CONTRATOS.md 7.7.3).
 *
 * Disparador: el consumidor de RabbitMQ de la cola `evaluacion.preguntas`
 * para la routing key `pregunta.archivada` (CONTRATOS.md 7.1 y 7.5).
 *
 * Idempotente por `idEvento` con "reclamar y liberar si falla"
 * (CONTRATOS.md 7.7.2): ver `RegistrarPreguntaPublicadaCasoUso` para el
 * detalle de la estrategia.
 */
export class RegistrarPreguntaArchivadaCasoUso
  implements CasoUso<RegistrarPreguntaArchivadaComando, ResultadoRegistroEvento>
{
  constructor(
    private readonly preguntaEvaluableRepositorio: PreguntaEvaluableRepositorio,
    private readonly registroEventosProcesados: RegistroEventosProcesadosPuerto,
    private readonly reloj: RelojPuerto,
  ) {}

  public async ejecutar(comando: RegistrarPreguntaArchivadaComando): Promise<ResultadoRegistroEvento> {
    const reclamado = await this.registroEventosProcesados.reclamar(comando.idEvento);
    if (!reclamado) {
      return 'DUPLICADO';
    }

    try {
      const ahora = this.reloj.ahora();
      const existente = await this.preguntaEvaluableRepositorio.obtenerPorId(comando.preguntaId);
      const preguntaEvaluable = existente
        ? existente.archivar(comando.motivo, comando.fechaArchivado, ahora)
        : PreguntaEvaluable.marcaDeArchivo(comando.preguntaId, comando.motivo, comando.fechaArchivado, ahora);

      await this.preguntaEvaluableRepositorio.guardar(preguntaEvaluable);

      return 'REGISTRADO';
    } catch (error) {
      await this.registroEventosProcesados.liberar(comando.idEvento);
      throw error;
    }
  }
}
