package co.edu.unicauca.bancopreguntas.editorial.infraestructura.reloj;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Implementación de {@link RelojPuerto} con el reloj del sistema en UTC (CONTRATOS.md 4).
 *
 * <p>Trunca a milisegundos para que la fecha sea idéntica al leerla de PostgreSQL (precisión de microsegundos) y
 * se serialice de forma compacta en ISO-8601 con {@code Z}.</p>
 */
public class RelojUtc implements RelojPuerto {

    private final Clock reloj;

    /**
     * Crea el reloj.
     *
     * @param reloj reloj base en UTC
     */
    public RelojUtc(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public Instant ahora() {
        return reloj.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
