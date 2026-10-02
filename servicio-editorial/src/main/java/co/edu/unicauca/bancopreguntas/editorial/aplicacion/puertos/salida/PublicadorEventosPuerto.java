package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida;

import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EventoDeDominio;

import java.util.List;

/**
 * Entrega los eventos de dominio que los agregados acumularon, después de guardarlos.
 *
 * <p>En la etapa 2 el adaptador RabbitMQ traduce {@code PreguntaPublicada} y {@code PreguntaArchivada} a los
 * mensajes de CONTRATOS.md 7.4 y 7.5 y los publica después del commit (7.7). Los demás eventos no salen
 * del servicio.</p>
 */
public interface PublicadorEventosPuerto {

    /**
     * Publica los eventos en el orden recibido.
     *
     * @param eventos eventos extraídos de los agregados; puede estar vacía
     */
    void publicar(List<EventoDeDominio> eventos);
}
