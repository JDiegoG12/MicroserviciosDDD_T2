package co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria;

import java.time.Instant;

/**
 * Sobre común de todos los eventos de integración (CONTRATOS.md 7.3). El orden de los componentes es el orden de
 * los campos en el JSON. Un campo opcional ausente se envía como {@code null}, no se omite (CONTRATOS.md 4).
 *
 * @param idEvento        UUID único del mensaje
 * @param tipoEvento      {@code PreguntaPublicada} o {@code PreguntaArchivada}
 * @param versionEvento   siempre 1
 * @param fechaOcurrencia cuándo ocurrió el hecho en el dominio
 * @param origen          siempre {@code servicio-editorial}
 * @param idCorrelacion   correlación de la petición HTTP, o {@code null}
 * @param datos           datos propios del evento (7.4 o 7.5)
 * @param <T>             tipo de los datos
 */
public record SobreEvento<T>(String idEvento, String tipoEvento, int versionEvento, Instant fechaOcurrencia,
                             String origen, String idCorrelacion, T datos) {
}
