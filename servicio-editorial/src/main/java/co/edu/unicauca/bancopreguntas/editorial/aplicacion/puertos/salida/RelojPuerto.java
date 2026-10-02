package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida;

import java.time.Instant;

/**
 * Fuente de la hora actual. Los casos de uso la consultan y pasan la fecha al dominio como parámetro,
 * para que el dominio nunca lea el reloj del sistema y las pruebas sean deterministas.
 */
public interface RelojPuerto {

    /**
     * Devuelve el instante actual en UTC (CONTRATOS.md 4).
     *
     * @return instante actual
     */
    Instant ahora();
}
