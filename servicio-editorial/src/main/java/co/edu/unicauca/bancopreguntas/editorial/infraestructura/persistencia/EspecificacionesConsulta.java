package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.AlcanceDeVisibilidad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.CriteriosBusquedaPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.EstadoProceso;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.AsignacionEmbebida;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.PreguntaEntidad;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.ProcesoRevisionEntidad;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filtros de las consultas paginadas, traducidos a SQL con JPA Criteria para que la base de datos los resuelva
 * (CONTRATOS.md 8.1: los filtros y la paginación se aplican en la base de datos, no en memoria).
 */
public final class EspecificacionesConsulta {

    private static final String CAMPO_ASIGNACIONES = "asignaciones";
    private static final String CAMPO_REVISOR = "revisorId";
    private static final String CAMPO_ESTADO = "estado";

    private EspecificacionesConsulta() {
    }

    /**
     * Preguntas visibles para el alcance (unión de roles) que cumplen los filtros de {@code GET /preguntas}.
     *
     * @param criterios filtros; los nulos no restringen
     * @param alcance   unión de visibilidad por rol (CU-06, D-08)
     * @return especificación combinada
     */
    public static Specification<PreguntaEntidad> preguntas(CriteriosBusquedaPregunta criterios, AlcanceDeVisibilidad alcance) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>(filtros(criterios, raiz, cb));
            if (!alcance.sinRestriccion()) {
                condiciones.add(unionDeVisibilidad(alcance, raiz, consulta, cb));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    /**
     * Procesos en un estado y, si se indica, con un Revisor asignado.
     *
     * @param estado    estado del proceso
     * @param revisorId Revisor o {@code null}
     * @return especificación
     */
    public static Specification<ProcesoRevisionEntidad> procesos(EstadoProceso estado, UUID revisorId) {
        return (raiz, consulta, cb) -> {
            Predicate delEstado = cb.equal(raiz.get(CAMPO_ESTADO), estado);
            if (revisorId == null) {
                return delEstado;
            }
            Subquery<UUID> asignado = consulta.subquery(UUID.class);
            Root<ProcesoRevisionEntidad> proceso = asignado.from(ProcesoRevisionEntidad.class);
            Join<ProcesoRevisionEntidad, AsignacionEmbebida> asignacion = proceso.join(CAMPO_ASIGNACIONES);
            asignado.select(proceso.get("id"))
                    .where(cb.equal(proceso.get("id"), raiz.get("id")), cb.equal(asignacion.get(CAMPO_REVISOR), revisorId));
            return cb.and(delEstado, cb.exists(asignado));
        };
    }

    // CONTRATOS.md 8.1: filtros competenciaId, temaId, subtemaId, nivelDificultad, estado y autorId.
    private static List<Predicate> filtros(CriteriosBusquedaPregunta criterios, Root<PreguntaEntidad> raiz, CriteriaBuilder cb) {
        List<Predicate> condiciones = new ArrayList<>();
        agregarSiPresente(condiciones, cb, raiz, "competenciaId", criterios.competenciaId());
        agregarSiPresente(condiciones, cb, raiz, "temaId", criterios.temaId());
        agregarSiPresente(condiciones, cb, raiz, "subtemaId", criterios.subtemaId());
        agregarSiPresente(condiciones, cb, raiz, "nivelDificultad", criterios.nivelDificultad());
        agregarSiPresente(condiciones, cb, raiz, CAMPO_ESTADO, criterios.estado());
        agregarSiPresente(condiciones, cb, raiz, "autorId", criterios.autorId() == null ? null : criterios.autorId().valor());
        return condiciones;
    }

    // CONTRATOS.md 8.1 y D-08: la visibilidad es la unión (O) de lo que permite cada rol.
    private static Predicate unionDeVisibilidad(AlcanceDeVisibilidad alcance, Root<PreguntaEntidad> raiz,
                                                CriteriaQuery<?> consulta, CriteriaBuilder cb) {
        List<Predicate> partes = new ArrayList<>();
        if (alcance.autorPropio() != null) {
            partes.add(cb.equal(raiz.get("autorId"), alcance.autorPropio().valor()));
        }
        if (alcance.publicadas()) {
            partes.add(cb.equal(raiz.get(CAMPO_ESTADO), EstadoPregunta.PUBLICADA));
        }
        if (alcance.revisorAsignado() != null) {
            partes.add(cb.exists(preguntaAsignada(alcance.revisorAsignado().valor(), raiz, consulta, cb)));
        }
        return partes.isEmpty() ? cb.disjunction() : cb.or(partes.toArray(Predicate[]::new));
    }

    // CU-06: "asignadas" = la Pregunta tiene un Proceso ABIERTO donde el Revisor está asignado (Taller 1, 12.1 y 12.2).
    private static Subquery<UUID> preguntaAsignada(UUID revisorId, Root<PreguntaEntidad> raiz,
                                                   CriteriaQuery<?> consulta, CriteriaBuilder cb) {
        Subquery<UUID> subconsulta = consulta.subquery(UUID.class);
        Root<ProcesoRevisionEntidad> proceso = subconsulta.from(ProcesoRevisionEntidad.class);
        Join<ProcesoRevisionEntidad, AsignacionEmbebida> asignacion = proceso.join(CAMPO_ASIGNACIONES);
        return subconsulta.select(proceso.get("preguntaId")).where(
                cb.equal(proceso.get("preguntaId"), raiz.get("id")),
                cb.equal(proceso.get(CAMPO_ESTADO), EstadoProceso.ABIERTO),
                cb.equal(asignacion.get(CAMPO_REVISOR), revisorId));
    }

    private static void agregarSiPresente(List<Predicate> condiciones, CriteriaBuilder cb, Root<PreguntaEntidad> raiz,
                                          String campo, Object valor) {
        if (valor != null) {
            condiciones.add(cb.equal(raiz.get(campo), valor));
        }
    }
}
