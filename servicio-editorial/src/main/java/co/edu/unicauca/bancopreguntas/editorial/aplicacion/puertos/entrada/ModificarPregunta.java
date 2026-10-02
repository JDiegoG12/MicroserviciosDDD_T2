package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ModificarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;

/**
 * Puerto de entrada de CU-05, Modificar pregunta ({@code PUT /preguntas/{preguntaId}}, rol {@code AUTOR} dueño).
 */
public interface ModificarPregunta {

    /**
     * Modifica la Pregunta, revalida y ajusta su estado.
     *
     * @param usuario quien la modifica
     * @param comando identificador y componentes nuevos
     * @return la Pregunta modificada
     */
    PreguntaRespuesta ejecutar(UsuarioActual usuario, ModificarPreguntaComando comando);
}
