package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.AlcanceDeVisibilidad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.CriteriosBusquedaPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.PreguntaEntidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JPA de {@link PreguntaRepositorio} sobre PostgreSQL (CONTRATOS.md 3.4, sufijo {@code Jpa}).
 *
 * <p>Debe invocarse dentro de la transacción que abre el decorador de cada caso de uso: así la entidad cargada en
 * {@link #obtenerPorId} es la misma que se actualiza en {@link #guardar} y el bloqueo optimista compara la versión
 * leída al inicio.</p>
 */
public class PreguntaRepositorioJpa implements PreguntaRepositorio {

    /** Orden estable de la paginación: fecha de creación y luego identificador. */
    static final Sort ORDEN = Sort.by(Sort.Order.asc("fechaCreacion"), Sort.Order.asc("id"));

    private final PreguntaJpaSpring repositorio;

    /**
     * Crea el repositorio.
     *
     * @param repositorio repositorio técnico de Spring Data
     */
    public PreguntaRepositorioJpa(PreguntaJpaSpring repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(Pregunta pregunta) {
        PreguntaEntidad entidad = repositorio.findById(pregunta.getId().valor())
                .orElseGet(() -> new PreguntaEntidad(pregunta.getId().valor()));
        MapeadorPreguntaJpa.copiarAEntidad(pregunta, entidad);
        repositorio.save(entidad);
    }

    @Override
    public Optional<Pregunta> obtenerPorId(PreguntaId preguntaId) {
        return repositorio.findById(preguntaId.valor()).map(MapeadorPreguntaJpa::aDominio);
    }

    @Override
    public Pagina<Pregunta> buscarPorCriterios(CriteriosBusquedaPregunta criterios, AlcanceDeVisibilidad alcance,
                                               Paginacion paginacion) {
        if (alcance.estaVacio()) {
            return Pagina.desdeLista(List.of(), paginacion);
        }
        Page<PreguntaEntidad> pagina = repositorio.findAll(EspecificacionesConsulta.preguntas(criterios, alcance),
                PageRequest.of(paginacion.pagina(), paginacion.tamano(), ORDEN));
        return new Pagina<>(pagina.getContent().stream().map(MapeadorPreguntaJpa::aDominio).toList(),
                paginacion.pagina(), paginacion.tamano(), pagina.getTotalElements(),
                Pagina.calcularTotalPaginas(pagina.getTotalElements(), paginacion.tamano()));
    }

    @Override
    public List<Pregunta> buscarPorIds(Collection<PreguntaId> preguntaIds) {
        return repositorio.findAllById(preguntaIds.stream().map(PreguntaId::valor).toList()).stream()
                .map(MapeadorPreguntaJpa::aDominio)
                .toList();
    }
}
