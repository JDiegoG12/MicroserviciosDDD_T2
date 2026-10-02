package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.AsignarRevisoresComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;

/**
 * Puerto de entrada de CU-10, Asignar revisores ({@code POST /preguntas/{preguntaId}/procesos-revision}).
 */
public interface AsignarRevisores {

    /**
     * Abre el Proceso de revisión y pasa la Pregunta a {@code EN_REVISION}.
     *
     * @param usuario Administrador
     * @param comando Pregunta y Revisores
     * @return el Proceso abierto
     */
    ProcesoRevisionRespuesta ejecutar(UsuarioActual usuario, AsignarRevisoresComando comando);
}
