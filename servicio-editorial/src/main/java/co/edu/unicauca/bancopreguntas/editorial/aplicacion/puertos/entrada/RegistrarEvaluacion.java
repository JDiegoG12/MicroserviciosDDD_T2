package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.RegistrarEvaluacionComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;

/**
 * Puerto de entrada de CU-11, Diligenciar formato de evaluación, con CU-12 automático
 * ({@code POST /procesos-revision/{procesoId}/evaluaciones}).
 */
public interface RegistrarEvaluacion {

    /**
     * Registra la evaluación y, si era la última, emite el Dictamen y resuelve la Pregunta.
     *
     * @param usuario Revisor asignado
     * @param comando evaluación
     * @return el Proceso actualizado
     */
    ProcesoRevisionRespuesta ejecutar(UsuarioActual usuario, RegistrarEvaluacionComando comando);
}
