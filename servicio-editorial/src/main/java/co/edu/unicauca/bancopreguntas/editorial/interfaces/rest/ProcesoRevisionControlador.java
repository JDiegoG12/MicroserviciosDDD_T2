package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarProcesosConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarProcesosRevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ObtenerProcesoRevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.RegistrarEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.EvaluacionSolicitud;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.MapeadorRest;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.PaginaJson;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.ProcesoRevisionJson;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores.ProblemaJson;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.openapi.EjemplosOpenApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Endpoints de procesos de revisión de CONTRATOS.md 8.1 ({@code /api/v1/procesos-revision}).
 */
@RestController
@RequestMapping(value = "/api/v1/procesos-revision", produces = "application/json")
@Tag(name = "Procesos de revisión", description = "Revisión por pares y dictamen (CU-06, CU-11, CU-12)")
public class ProcesoRevisionControlador {

    private static final String PROBLEMA = "application/problem+json";

    private final ObtenerProcesoRevision obtenerProcesoRevision;
    private final ConsultarProcesosRevision consultarProcesosRevision;
    private final RegistrarEvaluacion registrarEvaluacion;

    /**
     * Crea el controlador con los puertos de entrada.
     *
     * @param obtenerProcesoRevision    consulta de un proceso
     * @param consultarProcesosRevision CU-06 para revisores
     * @param registrarEvaluacion       CU-11 + CU-12
     */
    public ProcesoRevisionControlador(ObtenerProcesoRevision obtenerProcesoRevision,
                                      ConsultarProcesosRevision consultarProcesosRevision,
                                      RegistrarEvaluacion registrarEvaluacion) {
        this.obtenerProcesoRevision = obtenerProcesoRevision;
        this.consultarProcesosRevision = consultarProcesosRevision;
        this.registrarEvaluacion = registrarEvaluacion;
    }

    /**
     * Consulta un proceso. Roles {@code ADMINISTRADOR} o {@code REVISOR} asignado.
     *
     * @param usuario   identidad
     * @param procesoId UUID del proceso
     * @return 200 con el proceso
     */
    @GetMapping("/{procesoId}")
    @Operation(summary = "Obtener proceso de revisión", description = "Roles ADMINISTRADOR o REVISOR asignado.")
    @ApiResponse(responseCode = "200", description = "Proceso", content = @Content(
            schema = @Schema(implementation = ProcesoRevisionJson.class), examples = @ExampleObject(value = EjemplosOpenApi.PROCESO_RESPUESTA)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: sin rol válido o revisor no asignado", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "404", description = "PROCESO_REVISION_NO_ENCONTRADO", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public ProcesoRevisionJson obtener(@Parameter(hidden = true) UsuarioActual usuario, @PathVariable String procesoId) {
        return MapeadorRest.aJson(obtenerProcesoRevision.ejecutar(usuario, procesoId));
    }

    /**
     * CU-06 para revisores: procesos por estado y revisor, paginados.
     *
     * @param usuario   identidad
     * @param revisorId revisor (opcional)
     * @param estado    {@code ABIERTO} (por defecto) o {@code CERRADO}
     * @param pagina    página desde 0
     * @param tamano    tamaño de 1 a 100
     * @return 200 con la página de procesos
     */
    @GetMapping
    @Operation(summary = "Consultar procesos de revisión (CU-06)", description = "Roles REVISOR o ADMINISTRADOR. El REVISOR "
            + "solo ve sus procesos (si omite revisorId se usa el suyo). El ADMINISTRADOR puede omitir revisorId.")
    @ApiResponse(responseCode = "200", description = "Página de ProcesoRevisionRespuesta")
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: sin rol válido o revisor que pide los procesos de otro", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public PaginaJson<ProcesoRevisionJson> consultar(@Parameter(hidden = true) UsuarioActual usuario,
                                                     @RequestParam(required = false) String revisorId,
                                                     @RequestParam(required = false) @Parameter(schema = @Schema(allowableValues = {"ABIERTO", "CERRADO"})) String estado,
                                                     @RequestParam(required = false) @Parameter(description = "Desde 0") Integer pagina,
                                                     @RequestParam(required = false) @Parameter(description = "De 1 a 100; por defecto 20") Integer tamano) {
        return MapeadorRest.aJson(consultarProcesosRevision.ejecutar(usuario,
                new ConsultarProcesosConsulta(revisorId, estado, pagina, tamano)), MapeadorRest::aJson);
    }

    /**
     * CU-11 (+ CU-12 automático): registra la evaluación de un revisor asignado.
     *
     * @param usuario   identidad (rol {@code REVISOR})
     * @param procesoId UUID del proceso
     * @param solicitud formato de evaluación
     * @return 201 con {@code Location} y el proceso actualizado
     */
    @PostMapping(value = "/{procesoId}/evaluaciones", consumes = "application/json")
    @Operation(summary = "Registrar evaluación (CU-11, CU-12)", description = "Rol REVISOR asignado. Los tres criterios son "
            + "obligatorios con valoración 1–5. Si era la última evaluación pendiente, emite el dictamen (> 70 % aprueba, "
            + "INV-20) y aprueba o rechaza la pregunta en la misma transacción (D-15; excepción documentada a 3.3.5).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = EjemplosOpenApi.EVALUACION))))
    @ApiResponse(responseCode = "201", description = "Evaluación registrada", content = @Content(
            schema = @Schema(implementation = ProcesoRevisionJson.class), examples = @ExampleObject(value = EjemplosOpenApi.PROCESO_RESPUESTA)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO (sin rol REVISOR) o REVISOR_NO_ASIGNADO", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "404", description = "PROCESO_REVISION_NO_ENCONTRADO", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "409", description = "EVALUACION_YA_REGISTRADA (INV-18) o PROCESO_CERRADO (INV-21)", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public ResponseEntity<ProcesoRevisionJson> registrarEvaluacion(@Parameter(hidden = true) UsuarioActual usuario,
                                                                   @PathVariable String procesoId,
                                                                   @Valid @RequestBody EvaluacionSolicitud solicitud) {
        ProcesoRevisionRespuesta proceso = registrarEvaluacion.ejecutar(usuario, MapeadorRest.aComando(procesoId, solicitud));
        return ResponseEntity.created(URI.create("/api/v1/procesos-revision/" + proceso.procesoId()))
                .body(MapeadorRest.aJson(proceso));
    }
}
