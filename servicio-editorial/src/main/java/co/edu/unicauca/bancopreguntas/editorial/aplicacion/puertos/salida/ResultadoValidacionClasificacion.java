package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida;

/**
 * Respuesta de Catálogo a {@code ValidarClasificacion} (CONTRATOS.md 6, {@code ValidarClasificacionRespuesta}).
 *
 * @param valida  {@code true} si la terna existe y es coherente
 * @param motivo  primer motivo de rechazo encontrado; {@code NINGUNO} si es válida
 * @param detalle texto legible en español para mostrar al usuario
 */
public record ResultadoValidacionClasificacion(boolean valida, MotivoRechazoClasificacion motivo, String detalle) {

    /**
     * Resultado de una clasificación válida.
     *
     * @return resultado válido
     */
    public static ResultadoValidacionClasificacion valido() {
        return new ResultadoValidacionClasificacion(true, MotivoRechazoClasificacion.NINGUNO, "");
    }

    /**
     * Resultado de una clasificación inválida.
     *
     * @param motivo  motivo del rechazo
     * @param detalle texto legible
     * @return resultado inválido
     */
    public static ResultadoValidacionClasificacion invalido(MotivoRechazoClasificacion motivo, String detalle) {
        return new ResultadoValidacionClasificacion(false, motivo, detalle);
    }
}
