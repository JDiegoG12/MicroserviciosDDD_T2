package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.openapi;

/**
 * Ejemplos JSON de la documentación OpenAPI, con los identificadores de prueba de CONTRATOS.md 4.2 y 4.3.
 */
public final class EjemplosOpenApi {

    /** Cuerpo de {@code POST /preguntas} y {@code PUT /preguntas/{preguntaId}} (CONTRATOS.md 8.1). */
    public static final String PREGUNTA_SOLICITUD = """
            {
              "contexto": "Un grupo de 5 estudiantes obtuvo las notas 3,0; 3,5; 4,0; 4,0 y 4,5.",
              "preguntaDirecta": "¿Cuál es la moda del conjunto de notas?",
              "opciones": [
                { "letra": "A", "texto": "3,0", "esCorrecta": false },
                { "letra": "B", "texto": "3,8", "esCorrecta": false },
                { "letra": "C", "texto": "4,0", "esCorrecta": true },
                { "letra": "D", "texto": "4,5", "esCorrecta": false }
              ],
              "justificacion": "La moda es el valor que más se repite: 4,0 aparece dos veces.",
              "bibliografia": ["Walpole, R. Probabilidad y estadística. Pearson, 2012."],
              "clasificacion": {
                "competenciaId": "22222222-2222-4222-8222-000000000101",
                "temaId": "22222222-2222-4222-8222-000000000201",
                "subtemaId": "22222222-2222-4222-8222-000000000301"
              },
              "nivelDificultad": "BAJO"
            }""";

    /** Respuesta {@code PreguntaRespuesta}. */
    public static final String PREGUNTA_RESPUESTA = """
            {
              "preguntaId": "5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f",
              "autorId": "11111111-1111-4111-8111-000000000002",
              "contexto": "Un grupo de 5 estudiantes obtuvo las notas 3,0; 3,5; 4,0; 4,0 y 4,5.",
              "preguntaDirecta": "¿Cuál es la moda del conjunto de notas?",
              "opciones": [
                { "letra": "A", "texto": "3,0", "esCorrecta": false },
                { "letra": "B", "texto": "3,8", "esCorrecta": false },
                { "letra": "C", "texto": "4,0", "esCorrecta": true },
                { "letra": "D", "texto": "4,5", "esCorrecta": false }
              ],
              "justificacion": "La moda es el valor que más se repite: 4,0 aparece dos veces.",
              "bibliografia": ["Walpole, R. Probabilidad y estadística. Pearson, 2012."],
              "clasificacion": {
                "competenciaId": "22222222-2222-4222-8222-000000000101",
                "temaId": "22222222-2222-4222-8222-000000000201",
                "subtemaId": "22222222-2222-4222-8222-000000000301"
              },
              "nivelDificultad": "BAJO",
              "estado": "EN_CONSTRUCCION",
              "erroresValidacion": [],
              "fechaCreacion": "2026-10-01T15:30:00Z",
              "fechaActualizacion": "2026-10-01T15:30:00Z"
            }""";

    /** Página de {@code PreguntaResumen}. */
    public static final String PAGINA_PREGUNTAS = """
            {
              "contenido": [
                {
                  "preguntaId": "5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f",
                  "autorId": "11111111-1111-4111-8111-000000000002",
                  "preguntaDirecta": "¿Cuál es la moda del conjunto de notas?",
                  "estado": "PUBLICADA",
                  "clasificacion": {
                    "competenciaId": "22222222-2222-4222-8222-000000000101",
                    "temaId": "22222222-2222-4222-8222-000000000201",
                    "subtemaId": "22222222-2222-4222-8222-000000000301"
                  },
                  "nivelDificultad": "BAJO",
                  "fechaActualizacion": "2026-10-01T15:30:00Z"
                }
              ],
              "pagina": 0, "tamano": 20, "totalElementos": 1, "totalPaginas": 1
            }""";

    /** Cuerpo de {@code POST /preguntas/{preguntaId}/procesos-revision}. */
    public static final String ASIGNAR_REVISORES = """
            { "revisoresIds": ["11111111-1111-4111-8111-000000000003", "11111111-1111-4111-8111-000000000004"] }""";

    /** Cuerpo de {@code POST /procesos-revision/{procesoId}/evaluaciones}. */
    public static final String EVALUACION = """
            {
              "criterios": [
                { "criterio": "PEDAGOGICO", "valoracion": 4 },
                { "criterio": "TECNICO", "valoracion": 5 },
                { "criterio": "ESTRUCTURAL", "valoracion": 4 }
              ],
              "observaciones": ["El distractor B es poco plausible"],
              "decision": "APROBATORIA"
            }""";

    /** Respuesta {@code ProcesoRevisionRespuesta} con dictamen. */
    public static final String PROCESO_RESPUESTA = """
            {
              "procesoId": "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d",
              "preguntaId": "5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f",
              "estado": "CERRADO",
              "asignaciones": [
                { "revisorId": "11111111-1111-4111-8111-000000000003", "fechaAsignacion": "2026-10-01T15:32:00Z", "evaluacionRegistrada": true },
                { "revisorId": "11111111-1111-4111-8111-000000000004", "fechaAsignacion": "2026-10-01T15:32:00Z", "evaluacionRegistrada": true }
              ],
              "evaluaciones": [
                {
                  "revisorId": "11111111-1111-4111-8111-000000000003",
                  "criterios": [
                    { "criterio": "PEDAGOGICO", "valoracion": 4 },
                    { "criterio": "TECNICO", "valoracion": 5 },
                    { "criterio": "ESTRUCTURAL", "valoracion": 4 }
                  ],
                  "observaciones": ["El distractor B es poco plausible"],
                  "decision": "APROBATORIA",
                  "fechaEmision": "2026-10-01T15:40:00Z"
                }
              ],
              "dictamen": { "resultado": "APROBADA", "porcentajeAprobacion": 100.00, "fechaEmision": "2026-10-01T15:45:00Z" }
            }""";

    /** Cuerpo de {@code POST /preguntas/{preguntaId}/archivado}. */
    public static final String ARCHIVADO = """
            { "motivo": "Contenido desactualizado" }""";

    /** Respuesta {@code TrazabilidadRespuesta}. */
    public static final String TRAZABILIDAD = """
            {
              "preguntaId": "5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f",
              "registros": [
                { "fecha": "2026-10-01T15:30:00Z", "usuarioId": "11111111-1111-4111-8111-000000000002", "tipo": "CREACION",
                  "estadoAnterior": null, "estadoNuevo": "BORRADOR", "detalle": "Creación de la pregunta." },
                { "fecha": "2026-10-01T15:30:00Z", "usuarioId": "11111111-1111-4111-8111-000000000002", "tipo": "TRANSICION",
                  "estadoAnterior": "BORRADOR", "estadoNuevo": "EN_CONSTRUCCION", "detalle": "Supera la validación estructural (D-02)." }
              ],
              "historialRevisiones": [
                { "tipo": "DICTAMEN", "procesoId": "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d", "fecha": "2026-10-01T15:45:00Z",
                  "evaluacion": null,
                  "dictamen": { "resultado": "APROBADA", "porcentajeAprobacion": 100.00, "fechaEmision": "2026-10-01T15:45:00Z" } }
              ]
            }""";

    /** Error {@code application/problem+json} (CONTRATOS.md 5.2). */
    public static final String PROBLEMA = """
            {
              "type": "https://banco-preguntas/errores/TRANSICION_NO_PERMITIDA",
              "title": "Transición de estado no permitida",
              "status": 409,
              "detail": "La pregunta está en PUBLICADA y no puede pasar a EN_REVISION.",
              "instance": "/api/v1/preguntas/5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f/publicacion",
              "codigo": "TRANSICION_NO_PERMITIDA",
              "idCorrelacion": "0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b",
              "errores": []
            }""";

    private EjemplosOpenApi() {
    }
}
