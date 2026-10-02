package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.EstadoProceso;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.ProcesoRevisionEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio técnico de Spring Data sobre {@link ProcesoRevisionEntidad}. Solo lo usa
 * {@link ProcesoDeRevisionRepositorioJpa}.
 */
public interface ProcesoRevisionJpaSpring extends JpaRepository<ProcesoRevisionEntidad, UUID>,
        JpaSpecificationExecutor<ProcesoRevisionEntidad> {

    /**
     * Procesos de una Pregunta, del más antiguo al más reciente.
     *
     * @param preguntaId Pregunta
     * @return procesos en orden de apertura
     */
    List<ProcesoRevisionEntidad> findByPreguntaIdOrderByFechaAperturaAscIdAsc(UUID preguntaId);

    /**
     * Procesos en un estado donde un Revisor tiene Asignación.
     *
     * @param estado    estado del proceso
     * @param revisorId Revisor
     * @return procesos en orden de apertura
     */
    @Query("""
            select distinct p from ProcesoRevisionEntidad p join p.asignaciones a
            where p.estado = :estado and a.revisorId = :revisorId
            order by p.fechaApertura asc, p.id asc
            """)
    List<ProcesoRevisionEntidad> buscarPorEstadoYRevisorAsignado(@Param("estado") EstadoProceso estado,
                                                                 @Param("revisorId") UUID revisorId);
}
