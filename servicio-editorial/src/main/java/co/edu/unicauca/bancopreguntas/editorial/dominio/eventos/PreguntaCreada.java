package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.time.Instant;

/**
 * La Pregunta nació en el Banco de preguntas, en {@code BORRADOR} (CU-04, D-02; Taller 1, tabla 11.1).
 *
 * <p>Si en el mismo paso supera la validación estructural, después se emite
 * {@link ValidacionEstructuralSuperada}.</p>
 *
 * @param preguntaId      pregunta creada
 * @param autorId         Autor que la creó
 * @param fechaOcurrencia instante de la creación
 */
public record PreguntaCreada(PreguntaId preguntaId, UsuarioId autorId, Instant fechaOcurrencia)
        implements EventoDeDominio {
}
