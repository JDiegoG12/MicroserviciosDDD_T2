package co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos;

/**
 * Filtros de {@code GET /procesos-revision?revisorId={uuid}&estado=ABIERTO} (CU-06; CONTRATOS.md 8.1).
 *
 * @param revisorId UUID del Revisor
 * @param estado    estado del proceso; nulo equivale a {@code ABIERTO}
 * @param pagina    página desde 0 (por defecto 0)
 * @param tamano    tamaño de página (por defecto 20, máximo 100)
 */
public record ConsultarProcesosConsulta(String revisorId, String estado, Integer pagina, Integer tamano) {
}
