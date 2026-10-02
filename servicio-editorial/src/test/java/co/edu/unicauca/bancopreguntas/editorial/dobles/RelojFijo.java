package co.edu.unicauca.bancopreguntas.editorial.dobles;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;

import java.time.Duration;
import java.time.Instant;

/**
 * Doble de {@link RelojPuerto} con una hora fija que la prueba hace avanzar a mano.
 */
public final class RelojFijo implements RelojPuerto {

    private Instant actual;

    /**
     * Crea el reloj en un instante.
     *
     * @param inicial instante inicial
     */
    public RelojFijo(Instant inicial) {
        this.actual = inicial;
    }

    @Override
    public Instant ahora() {
        return actual;
    }

    /**
     * Avanza el reloj.
     *
     * @param minutos minutos a avanzar
     */
    public void avanzarMinutos(int minutos) {
        actual = actual.plus(Duration.ofMinutes(minutos));
    }
}
