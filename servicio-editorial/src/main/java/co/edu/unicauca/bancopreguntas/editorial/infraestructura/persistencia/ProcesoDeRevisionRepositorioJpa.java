package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.EstadoProceso;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.ProcesoRevisionEntidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

/**
 * Implementación JPA de {@link ProcesoDeRevisionRepositorio} sobre PostgreSQL (CONTRATOS.md 3.4, sufijo {@code Jpa}).
 */
public class ProcesoDeRevisionRepositorioJpa implements ProcesoDeRevisionRepositorio {

    private static final Sort ORDEN = Sort.by(Sort.Order.asc("fechaApertura"), Sort.Order.asc("id"));

    private final ProcesoRevisionJpaSpring repositorio;

    /**
     * Crea el repositorio.
     *
     * @param repositorio repositorio técnico de Spring Data
     */
    public ProcesoDeRevisionRepositorioJpa(ProcesoRevisionJpaSpring repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(ProcesoDeRevision proceso) {
        ProcesoRevisionEntidad entidad = repositorio.findById(proceso.getId().valor())
                .orElseGet(() -> MapeadorProcesoJpa.nuevaEntidad(proceso));
        MapeadorProcesoJpa.copiarAEntidad(proceso, entidad);
        repositorio.save(entidad);
    }

    @Override
    public Optional<ProcesoDeRevision> obtenerPorId(ProcesoDeRevisionId procesoId) {
        return repositorio.findById(procesoId.valor()).map(MapeadorProcesoJpa::aDominio);
    }

    // Taller 1, 12.2: el vigente es el ABIERTO o, si no hay, el último cerrado.
    @Override
    public Optional<ProcesoDeRevision> obtenerVigentePorPregunta(PreguntaId preguntaId) {
        List<ProcesoRevisionEntidad> historicos = repositorio.findByPreguntaIdOrderByFechaAperturaAscIdAsc(preguntaId.valor());
        return historicos.stream()
                .filter(proceso -> proceso.getEstado() == EstadoProceso.ABIERTO)
                .findFirst()
                .or(() -> historicos.isEmpty() ? Optional.empty() : Optional.of(historicos.get(historicos.size() - 1)))
                .map(MapeadorProcesoJpa::aDominio);
    }

    @Override
    public List<ProcesoDeRevision> buscarHistoricosPorPregunta(PreguntaId preguntaId) {
        return repositorio.findByPreguntaIdOrderByFechaAperturaAscIdAsc(preguntaId.valor()).stream()
                .map(MapeadorProcesoJpa::aDominio)
                .toList();
    }

    @Override
    public List<ProcesoDeRevision> buscarActivosPorRevisor(UsuarioId revisorId) {
        return repositorio.buscarPorEstadoYRevisorAsignado(EstadoProceso.ABIERTO, revisorId.valor()).stream()
                .map(MapeadorProcesoJpa::aDominio)
                .toList();
    }

    @Override
    public Pagina<ProcesoDeRevision> buscarPorEstadoYRevisor(EstadoProceso estado, UsuarioId revisorId, Paginacion paginacion) {
        Page<ProcesoRevisionEntidad> pagina = repositorio.findAll(
                EspecificacionesConsulta.procesos(estado, revisorId == null ? null : revisorId.valor()),
                PageRequest.of(paginacion.pagina(), paginacion.tamano(), ORDEN));
        return new Pagina<>(pagina.getContent().stream().map(MapeadorProcesoJpa::aDominio).toList(),
                paginacion.pagina(), paginacion.tamano(), pagina.getTotalElements(),
                Pagina.calcularTotalPaginas(pagina.getTotalElements(), paginacion.tamano()));
    }
}
