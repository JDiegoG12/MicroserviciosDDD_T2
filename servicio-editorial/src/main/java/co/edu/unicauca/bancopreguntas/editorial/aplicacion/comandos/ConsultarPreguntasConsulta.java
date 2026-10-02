package co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos;

/**
 * Filtros de {@code GET /preguntas} (CU-06; CONTRATOS.md 8.1). Un filtro nulo no restringe.
 *
 * @param competenciaId   UUID de la Competencia
 * @param temaId          UUID del Tema
 * @param subtemaId       UUID del Subtema
 * @param nivelDificultad nivel de dificultad
 * @param estado          estado del ciclo de vida
 * @param autorId         UUID del Autor
 * @param pagina          página desde 0 (por defecto 0)
 * @param tamano          tamaño de página (por defecto 20, máximo 100)
 */
public record ConsultarPreguntasConsulta(String competenciaId, String temaId, String subtemaId, String nivelDificultad,
                                         String estado, String autorId, Integer pagina, Integer tamano) {

    /**
     * Consulta sin filtros, con la paginación por defecto.
     *
     * @return consulta vacía
     */
    public static ConsultarPreguntasConsulta sinFiltros() {
        return new ConsultarPreguntasConsulta(null, null, null, null, null, null, null, null);
    }
}
