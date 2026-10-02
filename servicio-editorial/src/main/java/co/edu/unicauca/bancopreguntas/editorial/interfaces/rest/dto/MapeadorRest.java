package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.DatosDePreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.RegistrarEvaluacionComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ClasificacionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaResumen;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.TrazabilidadRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;

import java.util.List;
import java.util.function.Function;

/**
 * Traduce entre los DTOs JSON de la API y los comandos y resultados de la capa de aplicación. Los controladores no
 * tienen lógica de negocio (CONTRATOS.md 3.3.3): solo validan forma, llaman al caso de uso y traducen.
 */
public final class MapeadorRest {

    private MapeadorRest() {
    }

    /**
     * Convierte el cuerpo de pregunta en los datos del comando.
     *
     * @param solicitud cuerpo validado
     * @return datos del comando
     */
    public static DatosDePreguntaComando aDatos(PreguntaSolicitud solicitud) {
        PreguntaSolicitud.ClasificacionSolicitud clasificacion = solicitud.clasificacion();
        return new DatosDePreguntaComando(
                solicitud.contexto(),
                solicitud.preguntaDirecta(),
                solicitud.opciones() == null ? null : solicitud.opciones().stream()
                        .map(opcion -> new DatosDePreguntaComando.OpcionComando(opcion.letra(), opcion.texto(), opcion.esCorrecta()))
                        .toList(),
                solicitud.justificacion(),
                solicitud.bibliografia(),
                clasificacion.competenciaId(),
                clasificacion.temaId(),
                clasificacion.subtemaId(),
                solicitud.nivelDificultad());
    }

    /**
     * Convierte el cuerpo de evaluación en el comando.
     *
     * @param procesoId UUID del Proceso (de la ruta)
     * @param solicitud cuerpo validado
     * @return comando de evaluación
     */
    public static RegistrarEvaluacionComando aComando(String procesoId, EvaluacionSolicitud solicitud) {
        return new RegistrarEvaluacionComando(procesoId,
                solicitud.criterios().stream()
                        .map(criterio -> new RegistrarEvaluacionComando.CriterioComando(criterio.criterio(), criterio.valoracion()))
                        .toList(),
                solicitud.observaciones(),
                solicitud.decision());
    }

    /**
     * Convierte el resultado de pregunta en JSON.
     *
     * @param respuesta resultado del caso de uso
     * @return JSON de la API
     */
    public static PreguntaJson aJson(PreguntaRespuesta respuesta) {
        return new PreguntaJson(respuesta.preguntaId(), respuesta.autorId(), respuesta.contexto(), respuesta.preguntaDirecta(),
                respuesta.opciones().stream()
                        .map(opcion -> new PreguntaJson.OpcionJson(opcion.letra(), opcion.texto(), opcion.esCorrecta()))
                        .toList(),
                respuesta.justificacion(), respuesta.bibliografia(), aJson(respuesta.clasificacion()),
                respuesta.nivelDificultad(), respuesta.estado(),
                respuesta.erroresValidacion().stream()
                        .map(error -> new PreguntaJson.ErrorValidacionJson(error.regla(), error.mensaje()))
                        .toList(),
                respuesta.fechaCreacion(), respuesta.fechaActualizacion());
    }

    /**
     * Convierte el resumen de pregunta en JSON.
     *
     * @param resumen resultado del caso de uso
     * @return JSON de la API
     */
    public static PreguntaResumenJson aJson(PreguntaResumen resumen) {
        return new PreguntaResumenJson(resumen.preguntaId(), resumen.autorId(), resumen.preguntaDirecta(), resumen.estado(),
                aJson(resumen.clasificacion()), resumen.nivelDificultad(), resumen.fechaActualizacion());
    }

    /**
     * Convierte el resultado de proceso en JSON.
     *
     * @param proceso resultado del caso de uso
     * @return JSON de la API
     */
    public static ProcesoRevisionJson aJson(ProcesoRevisionRespuesta proceso) {
        return new ProcesoRevisionJson(proceso.procesoId(), proceso.preguntaId(), proceso.estado(),
                proceso.asignaciones().stream()
                        .map(asignacion -> new ProcesoRevisionJson.AsignacionJson(asignacion.revisorId(),
                                asignacion.fechaAsignacion(), asignacion.evaluacionRegistrada()))
                        .toList(),
                proceso.evaluaciones().stream().map(MapeadorRest::aJson).toList(),
                proceso.dictamen() == null ? null : aJson(proceso.dictamen()));
    }

    /**
     * Convierte el resultado de trazabilidad en JSON.
     *
     * @param trazabilidad resultado del caso de uso
     * @return JSON de la API
     */
    public static TrazabilidadJson aJson(TrazabilidadRespuesta trazabilidad) {
        return new TrazabilidadJson(trazabilidad.preguntaId(),
                trazabilidad.registros().stream()
                        .map(registro -> new TrazabilidadJson.RegistroJson(registro.fecha(), registro.usuarioId(), registro.tipo(),
                                registro.estadoAnterior(), registro.estadoNuevo(), registro.detalle()))
                        .toList(),
                trazabilidad.historialRevisiones().stream()
                        .map(entrada -> new TrazabilidadJson.EntradaHistorialJson(entrada.tipo(), entrada.procesoId(), entrada.fecha(),
                                entrada.evaluacion() == null ? null : aJson(entrada.evaluacion()),
                                entrada.dictamen() == null ? null : aJson(entrada.dictamen())))
                        .toList());
    }

    /**
     * Convierte una página de resultados en JSON (CONTRATOS.md 5.1).
     *
     * @param pagina         página del caso de uso
     * @param transformacion conversión de cada elemento
     * @param <T>            tipo de origen
     * @param <R>            tipo JSON
     * @return página JSON
     */
    public static <T, R> PaginaJson<R> aJson(Pagina<T> pagina, Function<T, R> transformacion) {
        List<R> contenido = pagina.contenido().stream().map(transformacion).toList();
        return new PaginaJson<>(contenido, pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas());
    }

    private static ClasificacionJson aJson(ClasificacionRespuesta clasificacion) {
        return new ClasificacionJson(clasificacion.competenciaId(), clasificacion.temaId(), clasificacion.subtemaId());
    }

    private static ProcesoRevisionJson.EvaluacionJson aJson(ProcesoRevisionRespuesta.EvaluacionRespuesta evaluacion) {
        return new ProcesoRevisionJson.EvaluacionJson(evaluacion.revisorId(),
                evaluacion.criterios().stream()
                        .map(criterio -> new ProcesoRevisionJson.CriterioJson(criterio.criterio(), criterio.valoracion()))
                        .toList(),
                evaluacion.observaciones(), evaluacion.decision(), evaluacion.fechaEmision());
    }

    private static ProcesoRevisionJson.DictamenJson aJson(ProcesoRevisionRespuesta.DictamenRespuesta dictamen) {
        return new ProcesoRevisionJson.DictamenJson(dictamen.resultado(), dictamen.porcentajeAprobacion(), dictamen.fechaEmision());
    }
}
