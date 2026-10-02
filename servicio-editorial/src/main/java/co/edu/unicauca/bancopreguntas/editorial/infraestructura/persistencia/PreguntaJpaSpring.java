package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia;

import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.PreguntaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repositorio técnico de Spring Data sobre {@link PreguntaEntidad}. Solo lo usa {@link PreguntaRepositorioJpa}:
 * el resto del sistema ve el {@code PreguntaRepositorio} del dominio, con métodos del negocio (CONTRATOS.md 3.3.7).
 */
public interface PreguntaJpaSpring extends JpaRepository<PreguntaEntidad, UUID>, JpaSpecificationExecutor<PreguntaEntidad> {
}
