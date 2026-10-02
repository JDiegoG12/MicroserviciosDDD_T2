package co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.AlcanceDeVisibilidad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.CriteriosBusquedaPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio del agregado Pregunta (Taller 1, sección 12.1). Guarda y recupera el agregado completo,
 * con su trazabilidad y su historial.
 *
 * <p>No ofrece ningún método de borrado: ninguna Pregunta se elimina físicamente (INV-12, RNF-16).</p>
 */
public interface PreguntaRepositorio {

    /**
     * Guarda la Pregunta completa (alta o actualización).
     *
     * @param pregunta agregado a guardar
     */
    void guardar(Pregunta pregunta);

    /**
     * Busca una Pregunta por su identidad.
     *
     * @param preguntaId identidad
     * @return la Pregunta o vacío si no existe
     */
    Optional<Pregunta> obtenerPorId(PreguntaId preguntaId);

    /**
     * Consulta paginada de CU-06 (CONTRATOS.md 8.1, {@code GET /preguntas}): primero la unión de visibilidad por
     * rol, luego los filtros sobre esa unión y por último la paginación del resultado combinado. Todo se resuelve
     * en la base de datos, no en memoria. Orden estable: fecha de creación y luego identificador.
     *
     * @param criterios  filtros; los nulos no restringen
     * @param alcance    Preguntas visibles para el usuario (unión de sus roles)
     * @param paginacion página solicitada
     * @return página de Preguntas visibles que cumplen los filtros
     */
    Pagina<Pregunta> buscarPorCriterios(CriteriosBusquedaPregunta criterios, AlcanceDeVisibilidad alcance,
                                        Paginacion paginacion);

    /**
     * Recupera varias Preguntas por identidad; sirve a CU-06 cuando el actor es un Revisor (Taller 1, 12.1).
     *
     * @param preguntaIds identidades buscadas
     * @return las Preguntas encontradas (las inexistentes se omiten)
     */
    List<Pregunta> buscarPorIds(Collection<PreguntaId> preguntaIds);
}
