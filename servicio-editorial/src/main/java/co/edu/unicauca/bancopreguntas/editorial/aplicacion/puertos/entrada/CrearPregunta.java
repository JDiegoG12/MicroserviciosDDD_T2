package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.DatosDePreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;

/**
 * Puerto de entrada de CU-04, Crear pregunta ({@code POST /preguntas}, rol {@code AUTOR}).
 */
public interface CrearPregunta {

    /**
     * Crea la Pregunta, valida su clasificación con Catálogo y la guarda.
     *
     * @param usuario quien la crea
     * @param datos   componentes de la Pregunta
     * @return la Pregunta creada ({@code BORRADOR} o {@code EN_CONSTRUCCION})
     */
    PreguntaRespuesta ejecutar(UsuarioActual usuario, DatosDePreguntaComando datos);
}
