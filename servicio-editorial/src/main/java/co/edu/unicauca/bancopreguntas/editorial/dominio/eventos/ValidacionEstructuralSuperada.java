package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.time.Instant;

/**
 * La Pregunta superó la validación estructural y pasó de {@code BORRADOR} a {@code EN_CONSTRUCCION}
 * (D-02, INV-11; Taller 1, tabla 11.1).
 *
 * @param preguntaId      pregunta validada
 * @param usuarioId       usuario cuya acción disparó la validación
 * @param fechaOcurrencia instante de la transición
 */
public record ValidacionEstructuralSuperada(PreguntaId preguntaId, UsuarioId usuarioId,
                                            Instant fechaOcurrencia) implements EventoDeDominio {
}
