package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import java.time.Instant;

/**
 * Hecho relevante, ya ocurrido e inmutable, que una raíz de agregado emite después de aplicar una
 * transición válida sobre sí misma (Taller 1, sección 11).
 *
 * <p>Los eventos de dominio no saben nada de RabbitMQ ni de JSON: la traducción al mensaje de
 * integración de la sección 7 de CONTRATOS.md ocurre en {@code infraestructura} (CONTRATOS.md 3.3.6).</p>
 */
public interface EventoDeDominio {

    /**
     * Indica cuándo ocurrió el hecho en el dominio (no cuándo se publica).
     *
     * @return instante UTC del hecho
     */
    Instant fechaOcurrencia();
}
