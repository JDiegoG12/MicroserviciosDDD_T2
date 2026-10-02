package co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ClasificacionAcademica;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ContenidoDePregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.DictamenEnHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EntradaDeHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EvaluacionEnHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.RegistroDeTrazabilidad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.CriterioEvaluado;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.DecisionRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Dictamen;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.FormatoDeEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Observacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Traduce los agregados del dominio a los DTOs de resultado de la capa de aplicación.
 */
public final class MapeadorDeResultados {

    private MapeadorDeResultados() {
    }

    /**
     * Convierte una Pregunta en {@code PreguntaRespuesta}.
     *
     * @param pregunta agregado
     * @return DTO completo, con los errores de validación vigentes
     */
    public static PreguntaRespuesta aPreguntaRespuesta(Pregunta pregunta) {
        ContenidoDePregunta contenido = pregunta.getContenido();
        return new PreguntaRespuesta(
                pregunta.getId().toString(),
                pregunta.getAutorId().toString(),
                contenido.contexto().texto(),
                contenido.preguntaDirecta().texto(),
                contenido.opciones().stream()
                        .map(opcion -> new PreguntaRespuesta.OpcionRespuesta(opcion.letra(), opcion.texto(), opcion.esCorrecta()))
                        .toList(),
                contenido.justificacion().texto(),
                contenido.bibliografia().referencias(),
                aClasificacion(contenido.clasificacion()),
                contenido.nivelDificultad().name(),
                pregunta.getEstado().name(),
                pregunta.erroresValidacion().stream()
                        .map(error -> new PreguntaRespuesta.ErrorValidacionRespuesta(error.regla(), error.mensaje()))
                        .toList(),
                pregunta.getFechaCreacion(),
                pregunta.getFechaActualizacion());
    }

    /**
     * Convierte una Pregunta en {@code PreguntaResumen}.
     *
     * @param pregunta agregado
     * @return resumen para listados
     */
    public static PreguntaResumen aPreguntaResumen(Pregunta pregunta) {
        ContenidoDePregunta contenido = pregunta.getContenido();
        return new PreguntaResumen(pregunta.getId().toString(), pregunta.getAutorId().toString(),
                contenido.preguntaDirecta().texto(), pregunta.getEstado().name(), aClasificacion(contenido.clasificacion()),
                contenido.nivelDificultad().name(), pregunta.getFechaActualizacion());
    }

    /**
     * Convierte un Proceso de revisión en {@code ProcesoRevisionRespuesta}.
     *
     * @param proceso agregado
     * @return DTO del proceso
     */
    public static ProcesoRevisionRespuesta aProcesoRevisionRespuesta(ProcesoDeRevision proceso) {
        return new ProcesoRevisionRespuesta(
                proceso.getId().toString(),
                proceso.getPreguntaId().toString(),
                proceso.getEstado().name(),
                proceso.getAsignaciones().stream()
                        .map(asignacion -> new ProcesoRevisionRespuesta.AsignacionRespuesta(
                                asignacion.revisorId().toString(), asignacion.fechaAsignacion(),
                                proceso.yaEvaluo(asignacion.revisorId())))
                        .toList(),
                proceso.getFormatos().stream().map(MapeadorDeResultados::aEvaluacion).toList(),
                proceso.getDictamen().map(MapeadorDeResultados::aDictamen).orElse(null));
    }

    /**
     * Arma la {@code TrazabilidadRespuesta} de una Pregunta (CU-18): registros e historial en orden cronológico.
     *
     * @param pregunta agregado
     * @return DTO de trazabilidad
     */
    public static TrazabilidadRespuesta aTrazabilidadRespuesta(Pregunta pregunta) {
        List<TrazabilidadRespuesta.RegistroRespuesta> registros = pregunta.getTrazabilidad().stream()
                .sorted(Comparator.comparing(RegistroDeTrazabilidad::fecha))
                .map(MapeadorDeResultados::aRegistro)
                .toList();
        List<TrazabilidadRespuesta.EntradaHistorialRespuesta> historial = pregunta.getHistorialRevisiones().entradas().stream()
                .sorted(Comparator.comparing(EntradaDeHistorial::fecha))
                .map(MapeadorDeResultados::aEntradaHistorial)
                .toList();
        return new TrazabilidadRespuesta(pregunta.getId().toString(), registros, historial);
    }

    private static ClasificacionRespuesta aClasificacion(ClasificacionAcademica clasificacion) {
        return new ClasificacionRespuesta(clasificacion.competenciaId().toString(), clasificacion.temaId().toString(),
                clasificacion.subtemaId().toString());
    }

    private static ProcesoRevisionRespuesta.EvaluacionRespuesta aEvaluacion(FormatoDeEvaluacion formato) {
        return aEvaluacion(formato.getRevisorId(), formato.getCriterios(), formato.getObservaciones(),
                formato.getDecision(), formato.getFechaEmision());
    }

    private static ProcesoRevisionRespuesta.EvaluacionRespuesta aEvaluacion(UsuarioId revisorId,
                                                                            List<CriterioEvaluado> criterios,
                                                                            List<Observacion> observaciones,
                                                                            DecisionRevision decision, Instant fecha) {
        return new ProcesoRevisionRespuesta.EvaluacionRespuesta(
                revisorId.toString(),
                criterios.stream()
                        .map(criterio -> new ProcesoRevisionRespuesta.CriterioRespuesta(criterio.criterio().name(), criterio.valoracion()))
                        .toList(),
                observaciones.stream().map(Observacion::texto).toList(),
                decision.name(),
                fecha);
    }

    private static ProcesoRevisionRespuesta.DictamenRespuesta aDictamen(Dictamen dictamen) {
        return new ProcesoRevisionRespuesta.DictamenRespuesta(dictamen.resultado().name(), dictamen.porcentajeAprobacion(),
                dictamen.fechaEmision());
    }

    private static TrazabilidadRespuesta.RegistroRespuesta aRegistro(RegistroDeTrazabilidad registro) {
        return new TrazabilidadRespuesta.RegistroRespuesta(registro.fecha(), registro.usuarioId().toString(),
                registro.tipo().name(), registro.estadoAnterior() == null ? null : registro.estadoAnterior().name(),
                registro.estadoNuevo().name(), registro.detalle());
    }

    private static TrazabilidadRespuesta.EntradaHistorialRespuesta aEntradaHistorial(EntradaDeHistorial entrada) {
        return switch (entrada) {
            case EvaluacionEnHistorial evaluacion -> new TrazabilidadRespuesta.EntradaHistorialRespuesta(
                    "EVALUACION", evaluacion.procesoId().toString(), evaluacion.fecha(),
                    aEvaluacion(evaluacion.revisorId(), evaluacion.criterios(), evaluacion.observaciones(),
                            evaluacion.decision(), evaluacion.fecha()),
                    null);
            case DictamenEnHistorial dictamen -> new TrazabilidadRespuesta.EntradaHistorialRespuesta(
                    "DICTAMEN", dictamen.procesoId().toString(), dictamen.fecha(), null, aDictamen(dictamen.dictamen()));
        };
    }
}
