package co.edu.unicauca.bancopreguntas.editorial.dobles;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.AlcanceDeVisibilidad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.CriteriosBusquedaPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Doble en memoria de {@link PreguntaRepositorio} para las pruebas de aplicación. Resuelve las Preguntas
 * "asignadas" de un Revisor con el doble de procesos, como hará la consulta SQL en la etapa 2.
 */
public final class PreguntaRepositorioEnMemoria implements PreguntaRepositorio {

    private final Map<PreguntaId, Pregunta> preguntas = new LinkedHashMap<>();
    private final ProcesoDeRevisionRepositorioEnMemoria procesos;
    private int cantidadDeGuardados;

    /**
     * Crea el doble.
     *
     * @param procesos doble de procesos, para resolver las Preguntas asignadas a un Revisor
     */
    public PreguntaRepositorioEnMemoria(ProcesoDeRevisionRepositorioEnMemoria procesos) {
        this.procesos = procesos;
    }

    @Override
    public void guardar(Pregunta pregunta) {
        preguntas.put(pregunta.getId(), pregunta);
        cantidadDeGuardados++;
    }

    @Override
    public Optional<Pregunta> obtenerPorId(PreguntaId preguntaId) {
        return Optional.ofNullable(preguntas.get(preguntaId));
    }

    @Override
    public Pagina<Pregunta> buscarPorCriterios(CriteriosBusquedaPregunta criterios, AlcanceDeVisibilidad alcance,
                                               Paginacion paginacion) {
        Set<PreguntaId> asignadas = alcance.revisorAsignado() == null ? Set.of()
                : procesos.buscarActivosPorRevisor(alcance.revisorAsignado()).stream()
                        .map(ProcesoDeRevision::getPreguntaId)
                        .collect(Collectors.toSet());
        List<Pregunta> filtradas = preguntas.values().stream()
                .filter(pregunta -> esVisible(pregunta, alcance, asignadas))
                .filter(criterios::seCumplenEn)
                .sorted(Comparator.comparing(Pregunta::getFechaCreacion))
                .toList();
        return Pagina.desdeLista(filtradas, paginacion);
    }

    private static boolean esVisible(Pregunta pregunta, AlcanceDeVisibilidad alcance, Set<PreguntaId> asignadas) {
        return alcance.sinRestriccion()
                || (alcance.autorPropio() != null && pregunta.esAutor(alcance.autorPropio()))
                || asignadas.contains(pregunta.getId())
                || (alcance.publicadas() && pregunta.getEstado() == EstadoPregunta.PUBLICADA);
    }

    @Override
    public List<Pregunta> buscarPorIds(Collection<PreguntaId> preguntaIds) {
        return preguntaIds.stream().map(preguntas::get).filter(Objects::nonNull).toList();
    }

    /**
     * Cuántas veces se llamó a {@link #guardar}.
     *
     * @return cantidad de guardados
     */
    public int cantidadDeGuardados() {
        return cantidadDeGuardados;
    }

    /**
     * Cuántas Preguntas distintas hay guardadas.
     *
     * @return cantidad de Preguntas
     */
    public int cantidad() {
        return preguntas.size();
    }
}
