package co.edu.unicauca.bancopreguntas.editorial.dobles;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EventoDeDominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Doble de {@link PublicadorEventosPuerto} que guarda los eventos publicados en orden.
 */
public final class PublicadorEventosEnMemoria implements PublicadorEventosPuerto {

    private final List<EventoDeDominio> publicados = new ArrayList<>();

    @Override
    public void publicar(List<EventoDeDominio> eventos) {
        publicados.addAll(eventos);
    }

    /**
     * Todos los eventos publicados.
     *
     * @return copia inmutable
     */
    public List<EventoDeDominio> publicados() {
        return List.copyOf(publicados);
    }

    /**
     * Los eventos publicados de un tipo.
     *
     * @param tipo clase del evento
     * @param <E>  tipo del evento
     * @return eventos de ese tipo, en orden
     */
    public <E extends EventoDeDominio> List<E> publicadosDeTipo(Class<E> tipo) {
        return publicados.stream().filter(tipo::isInstance).map(tipo::cast).toList();
    }

    /**
     * Olvida los eventos publicados hasta ahora.
     */
    public void limpiar() {
        publicados.clear();
    }
}
