package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ArchivarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.AsignarRevisoresComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarPreguntasConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ModificarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ArchivarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.AsignarRevisores;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarPreguntas;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarTrazabilidad;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.CrearPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.EnviarPreguntaARevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ModificarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ObtenerPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.PublicarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.ArchivadoSolicitud;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.AsignarRevisoresSolicitud;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.MapeadorRest;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.PaginaJson;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.PreguntaJson;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.PreguntaResumenJson;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.PreguntaSolicitud;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.ProcesoRevisionJson;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto.TrazabilidadJson;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Endpoints de preguntas de CONTRATOS.md 8.1 ({@code /api/v1/preguntas}). Sin reglas de negocio (3.3.3): arma el
 * comando, llama al puerto de entrada y traduce la respuesta.
 */
@RestController
@RequestMapping(value = "/api/v1/preguntas", produces = "application/json")
@Tag(name = "Preguntas", description = "Ciclo de vida de la Pregunta (CU-04 a CU-10, CU-18)")
public class PreguntaControlador {

    private static final String PROBLEMA = "application/problem+json";

    private final CrearPregunta crearPregunta;
    private final ModificarPregunta modificarPregunta;
    private final ConsultarPreguntas consultarPreguntas;
    private final ObtenerPregunta obtenerPregunta;
    private final EnviarPreguntaARevision enviarPreguntaARevision;
    private final AsignarRevisores asignarRevisores;
    private final PublicarPregunta publicarPregunta;
    private final ArchivarPregunta archivarPregunta;
    private final ConsultarTrazabilidad consultarTrazabilidad;

    /**
     * Crea el controlador con los puertos de entrada.
     *
     * @param crearPregunta           CU-04
     * @param modificarPregunta       CU-05
     * @param consultarPreguntas      CU-06
     * @param obtenerPregunta         CU-06 (una)
     * @param enviarPreguntaARevision CU-07
     * @param asignarRevisores        CU-10
     * @param publicarPregunta        CU-08
     * @param archivarPregunta        CU-09
     * @param consultarTrazabilidad   CU-18
     */
    public PreguntaControlador(CrearPregunta crearPregunta, ModificarPregunta modificarPregunta,
                               ConsultarPreguntas consultarPreguntas, ObtenerPregunta obtenerPregunta,
                               EnviarPreguntaARevision enviarPreguntaARevision, AsignarRevisores asignarRevisores,
                               PublicarPregunta publicarPregunta, ArchivarPregunta archivarPregunta,
                               ConsultarTrazabilidad consultarTrazabilidad) {
        this.crearPregunta = crearPregunta;
        this.modificarPregunta = modificarPregunta;
        this.consultarPreguntas = consultarPreguntas;
        this.obtenerPregunta = obtenerPregunta;
        this.enviarPreguntaARevision = enviarPreguntaARevision;
        this.asignarRevisores = asignarRevisores;
        this.publicarPregunta = publicarPregunta;
        this.archivarPregunta = archivarPregunta;
        this.consultarTrazabilidad = consultarTrazabilidad;
    }

    /**
     * CU-04: crea una pregunta. Rol {@code AUTOR}.
     *
     * @param usuario   identidad de {@code X-Usuario-Id} y {@code X-Roles}
     * @param solicitud componentes de la pregunta
     * @return 201 con {@code Location} y la pregunta ({@code BORRADOR} o {@code EN_CONSTRUCCION})
     */
    @PostMapping(consumes = "application/json")
    @Operation(summary = "Crear pregunta (CU-04)", description = "Rol AUTOR. Nace en BORRADOR y, si supera la validación "
            + "estructural (RF-08 a RF-13, INV-04), pasa a EN_CONSTRUCCION en la misma petición. Valida la clasificación con "
            + "Catálogo por gRPC antes de guardar.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = EjemplosOpenApi.PREGUNTA_SOLICITUD))))
    @ApiResponse(responseCode = "201", description = "Pregunta creada", content = @Content(
            schema = @Schema(implementation = PreguntaJson.class), examples = @ExampleObject(value = EjemplosOpenApi.PREGUNTA_RESPUESTA)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: el usuario no tiene el rol AUTOR", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "422", description = "CLASIFICACION_INVALIDA: Catálogo rechazó la terna", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "503", description = "CATALOGO_NO_DISPONIBLE o BASE_DE_DATOS_NO_DISPONIBLE; la pregunta no se guarda", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public ResponseEntity<PreguntaJson> crear(@Parameter(hidden = true) UsuarioActual usuario,
                                              @Valid @RequestBody PreguntaSolicitud solicitud) {
        PreguntaRespuesta creada = crearPregunta.ejecutar(usuario, MapeadorRest.aDatos(solicitud));
        return ResponseEntity.created(URI.create("/api/v1/preguntas/" + creada.preguntaId())).body(MapeadorRest.aJson(creada));
    }

    /**
     * CU-05: modifica una pregunta. Rol {@code AUTOR} dueño.
     *
     * @param usuario    identidad
     * @param preguntaId UUID de la pregunta
     * @param solicitud  componentes nuevos
     * @return 200 con la pregunta revalidada
     */
    @PutMapping(value = "/{preguntaId}", consumes = "application/json")
    @Operation(summary = "Modificar pregunta (CU-05)", description = "Rol AUTOR (dueño). Solo en BORRADOR o EN_CONSTRUCCION "
            + "(INV-10). Revalida y ajusta el estado (INV-11).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = EjemplosOpenApi.PREGUNTA_SOLICITUD))))
    @ApiResponse(responseCode = "200", description = "Pregunta modificada", content = @Content(
            schema = @Schema(implementation = PreguntaJson.class), examples = @ExampleObject(value = EjemplosOpenApi.PREGUNTA_RESPUESTA)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: sin rol AUTOR o no es el autor", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "404", description = "PREGUNTA_NO_ENCONTRADA", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "409", description = "PREGUNTA_NO_EDITABLE (INV-10)", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "422", description = "CLASIFICACION_INVALIDA", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "503", description = "CATALOGO_NO_DISPONIBLE o BASE_DE_DATOS_NO_DISPONIBLE", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public PreguntaJson modificar(@Parameter(hidden = true) UsuarioActual usuario, @PathVariable String preguntaId,
                                  @Valid @RequestBody PreguntaSolicitud solicitud) {
        return MapeadorRest.aJson(modificarPregunta.ejecutar(usuario,
                new ModificarPreguntaComando(preguntaId, MapeadorRest.aDatos(solicitud))));
    }

    /**
     * CU-06: consulta paginada con filtros y restricción por rol (unión de roles).
     *
     * @param usuario         identidad
     * @param competenciaId   filtro opcional
     * @param temaId          filtro opcional
     * @param subtemaId       filtro opcional
     * @param nivelDificultad filtro opcional
     * @param estado          filtro opcional
     * @param autorId         filtro opcional
     * @param pagina          página desde 0
     * @param tamano          tamaño de 1 a 100
     * @return 200 con la página de {@code PreguntaResumen}
     */
    @GetMapping
    @Operation(summary = "Consultar preguntas (CU-06)", description = "Cualquier rol salvo solo ESTUDIANTE. ADMINISTRADOR ve "
            + "todas; si no, la unión de lo que permite cada rol: AUTOR sus preguntas, REVISOR las de sus procesos abiertos y "
            + "DOCENTE las PUBLICADA. Los filtros y la paginación se aplican sobre esa unión.")
    @ApiResponse(responseCode = "200", description = "Página de PreguntaResumen", content = @Content(
            examples = @ExampleObject(value = EjemplosOpenApi.PAGINA_PREGUNTAS)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: el usuario solo tiene el rol ESTUDIANTE", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public PaginaJson<PreguntaResumenJson> consultar(@Parameter(hidden = true) UsuarioActual usuario,
                                                     @RequestParam(required = false) String competenciaId,
                                                     @RequestParam(required = false) String temaId,
                                                     @RequestParam(required = false) String subtemaId,
                                                     @RequestParam(required = false) @Parameter(schema = @Schema(allowableValues = {"BAJO", "MEDIO", "ALTO"})) String nivelDificultad,
                                                     @RequestParam(required = false) String estado,
                                                     @RequestParam(required = false) String autorId,
                                                     @RequestParam(required = false) @Parameter(description = "Desde 0") Integer pagina,
                                                     @RequestParam(required = false) @Parameter(description = "De 1 a 100; por defecto 20") Integer tamano) {
        ConsultarPreguntasConsulta consulta = new ConsultarPreguntasConsulta(competenciaId, temaId, subtemaId, nivelDificultad,
                estado, autorId, pagina, tamano);
        return MapeadorRest.aJson(consultarPreguntas.ejecutar(usuario, consulta), MapeadorRest::aJson);
    }

    /**
     * CU-06: consulta una pregunta si algún rol del usuario la puede ver.
     *
     * @param usuario    identidad
     * @param preguntaId UUID de la pregunta
     * @return 200 con la pregunta
     */
    @GetMapping("/{preguntaId}")
    @Operation(summary = "Obtener pregunta (CU-06)", description = "Visible según el rol: ADMINISTRADOR siempre, AUTOR si es "
            + "suya, REVISOR si está asignado a su proceso vigente, DOCENTE si está PUBLICADA.")
    @ApiResponse(responseCode = "200", description = "Pregunta", content = @Content(
            schema = @Schema(implementation = PreguntaJson.class), examples = @ExampleObject(value = EjemplosOpenApi.PREGUNTA_RESPUESTA)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: existe pero ningún rol del usuario permite verla", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "404", description = "PREGUNTA_NO_ENCONTRADA", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public PreguntaJson obtener(@Parameter(hidden = true) UsuarioActual usuario, @PathVariable String preguntaId) {
        return MapeadorRest.aJson(obtenerPregunta.ejecutar(usuario, preguntaId));
    }

    /**
     * CU-07: somete la pregunta a revisión. Rol {@code AUTOR} dueño.
     *
     * @param usuario    identidad
     * @param preguntaId UUID de la pregunta
     * @return 200 con la pregunta en {@code PENDIENTE_REVISION}
     */
    @PostMapping("/{preguntaId}/envio-revision")
    @Operation(summary = "Enviar a revisión (CU-07)", description = "Rol AUTOR (dueño). De EN_CONSTRUCCION a PENDIENTE_REVISION.")
    @ApiResponse(responseCode = "200", description = "Pregunta en PENDIENTE_REVISION", content = @Content(schema = @Schema(implementation = PreguntaJson.class)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: sin rol AUTOR o no es el autor", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "404", description = "PREGUNTA_NO_ENCONTRADA", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "409", description = "TRANSICION_NO_PERMITIDA: no está EN_CONSTRUCCION", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public PreguntaJson enviarARevision(@Parameter(hidden = true) UsuarioActual usuario, @PathVariable String preguntaId) {
        return MapeadorRest.aJson(enviarPreguntaARevision.ejecutar(usuario, preguntaId));
    }

    /**
     * CU-10: asigna revisores y abre el proceso de revisión. Rol {@code ADMINISTRADOR}.
     *
     * @param usuario    identidad
     * @param preguntaId UUID de la pregunta
     * @param solicitud  revisores
     * @return 201 con {@code Location} y el proceso abierto
     */
    @PostMapping(value = "/{preguntaId}/procesos-revision", consumes = "application/json")
    @Operation(summary = "Asignar revisores (CU-10)", description = "Rol ADMINISTRADOR. Abre el proceso y pasa la pregunta a "
            + "EN_REVISION en una sola transacción (D-14; excepción documentada a 3.3.5). Mínimo 2 revisores (INV-15), sin "
            + "repetir (INV-17), ninguno igual al autor (INV-16).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = EjemplosOpenApi.ASIGNAR_REVISORES))))
    @ApiResponse(responseCode = "201", description = "Proceso abierto", content = @Content(
            schema = @Schema(implementation = ProcesoRevisionJson.class), examples = @ExampleObject(value = EjemplosOpenApi.PROCESO_RESPUESTA)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: sin rol ADMINISTRADOR", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "404", description = "PREGUNTA_NO_ENCONTRADA", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "409", description = "TRANSICION_NO_PERMITIDA: la pregunta no está PENDIENTE_REVISION", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "422", description = "REVISORES_INSUFICIENTES, AUTOR_NO_PUEDE_SER_REVISOR o REVISOR_DUPLICADO", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public ResponseEntity<ProcesoRevisionJson> asignarRevisores(@Parameter(hidden = true) UsuarioActual usuario,
                                                                @PathVariable String preguntaId,
                                                                @Valid @RequestBody AsignarRevisoresSolicitud solicitud) {
        ProcesoRevisionRespuesta proceso = asignarRevisores.ejecutar(usuario,
                new AsignarRevisoresComando(preguntaId, solicitud.revisoresIds()));
        return ResponseEntity.created(URI.create("/api/v1/procesos-revision/" + proceso.procesoId()))
                .body(MapeadorRest.aJson(proceso));
    }

    /**
     * CU-08: publica la pregunta y emite {@code PreguntaPublicada}. Rol {@code ADMINISTRADOR}.
     *
     * @param usuario    identidad
     * @param preguntaId UUID de la pregunta
     * @return 200 con la pregunta publicada
     */
    @PostMapping("/{preguntaId}/publicacion")
    @Operation(summary = "Publicar pregunta (CU-08)", description = "Rol ADMINISTRADOR. De APROBADA a PUBLICADA, solo si el "
            + "proceso vigente cerró con dictamen APROBADA. Después del commit emite PreguntaPublicada (routing key "
            + "pregunta.publicada).")
    @ApiResponse(responseCode = "200", description = "Pregunta publicada", content = @Content(schema = @Schema(implementation = PreguntaJson.class)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: sin rol ADMINISTRADOR", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "404", description = "PREGUNTA_NO_ENCONTRADA", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "409", description = "TRANSICION_NO_PERMITIDA: no está APROBADA o no hay dictamen APROBADA", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class), examples = @ExampleObject(value = EjemplosOpenApi.PROBLEMA)))
    public PreguntaJson publicar(@Parameter(hidden = true) UsuarioActual usuario, @PathVariable String preguntaId) {
        return MapeadorRest.aJson(publicarPregunta.ejecutar(usuario, preguntaId));
    }

    /**
     * CU-09: archiva la pregunta y emite {@code PreguntaArchivada} con el motivo. Rol {@code ADMINISTRADOR}.
     *
     * @param usuario    identidad
     * @param preguntaId UUID de la pregunta
     * @param solicitud  motivo
     * @return 200 con la pregunta archivada
     */
    @PostMapping(value = "/{preguntaId}/archivado", consumes = "application/json")
    @Operation(summary = "Archivar pregunta (CU-09)", description = "Rol ADMINISTRADOR. De PUBLICADA a ARCHIVADA, sin borrarla "
            + "(INV-12). Después del commit emite PreguntaArchivada con el motivo (routing key pregunta.archivada).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = EjemplosOpenApi.ARCHIVADO))))
    @ApiResponse(responseCode = "200", description = "Pregunta archivada", content = @Content(schema = @Schema(implementation = PreguntaJson.class)))
    @ApiResponse(responseCode = "400", description = "SOLICITUD_INVALIDA: falta el motivo o supera 500 caracteres", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: sin rol ADMINISTRADOR", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "404", description = "PREGUNTA_NO_ENCONTRADA", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "409", description = "TRANSICION_NO_PERMITIDA: no está PUBLICADA", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public PreguntaJson archivar(@Parameter(hidden = true) UsuarioActual usuario, @PathVariable String preguntaId,
                                 @Valid @RequestBody ArchivadoSolicitud solicitud) {
        return MapeadorRest.aJson(archivarPregunta.ejecutar(usuario, new ArchivarPreguntaComando(preguntaId, solicitud.motivo())));
    }

    /**
     * CU-18: trazabilidad completa de la pregunta. Rol {@code ADMINISTRADOR}.
     *
     * @param usuario    identidad
     * @param preguntaId UUID de la pregunta
     * @return 200 con registros e historial en orden cronológico
     */
    @GetMapping("/{preguntaId}/trazabilidad")
    @Operation(summary = "Consultar trazabilidad (CU-18)", description = "Rol ADMINISTRADOR. Registros de trazabilidad e "
            + "historial de revisiones de todos los procesos, en orden cronológico.")
    @ApiResponse(responseCode = "200", description = "Trazabilidad", content = @Content(
            schema = @Schema(implementation = TrazabilidadJson.class), examples = @ExampleObject(value = EjemplosOpenApi.TRAZABILIDAD)))
    @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO: sin rol ADMINISTRADOR", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    @ApiResponse(responseCode = "404", description = "PREGUNTA_NO_ENCONTRADA", content = @Content(mediaType = PROBLEMA, schema = @Schema(implementation = ProblemaJson.class)))
    public TrazabilidadJson trazabilidad(@Parameter(hidden = true) UsuarioActual usuario, @PathVariable String preguntaId) {
        return MapeadorRest.aJson(consultarTrazabilidad.ejecutar(usuario, preguntaId));
    }
}
