package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun;

import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EventoDeDominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de las raíces de agregado: acumula los eventos de dominio en una lista interna.
 *
 * <p>El caso de uso los extrae con {@link #extraerEventos()} después de guardar el agregado y se los
 * entrega al publicador de eventos. El dominio nunca publica por su cuenta (CONTRATOS.md 3.3.6 y 11.1).</p>
 */
public abstract class RaizDeAgregado {

    private final List<EventoDeDominio> eventosPendientes = new ArrayList<>();

    /**
     * Anota un evento de dominio para que el caso de uso lo publique después de guardar.
     *
     * @param evento evento que acaba de ocurrir
     */
    protected void registrarEvento(EventoDeDominio evento) {
        eventosPendientes.add(evento);
    }

    /**
     * Devuelve los eventos acumulados y vacía la lista interna, para no publicarlos dos veces.
     *
     * @return eventos en el orden en que ocurrieron
     */
    public List<EventoDeDominio> extraerEventos() {
        List<EventoDeDominio> extraidos = List.copyOf(eventosPendientes);
        eventosPendientes.clear();
        return extraidos;
    }
}
