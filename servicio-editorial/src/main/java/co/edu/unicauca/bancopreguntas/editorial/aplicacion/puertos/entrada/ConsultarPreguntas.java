package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarPreguntasConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaResumen;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;

/**
 * Puerto de entrada de CU-06, Consultar preguntas mediante filtros ({@code GET /preguntas}).
 */
public interface ConsultarPreguntas {

    /**
     * Consulta las Preguntas visibles para el rol del usuario.
     *
     * @param usuario  quien consulta
     * @param consulta filtros y paginación
     * @return página de resúmenes
     */
    Pagina<PreguntaResumen> ejecutar(UsuarioActual usuario, ConsultarPreguntasConsulta consulta);
}
