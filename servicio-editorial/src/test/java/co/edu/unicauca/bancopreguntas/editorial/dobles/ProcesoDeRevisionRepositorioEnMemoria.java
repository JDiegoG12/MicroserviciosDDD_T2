package co.edu.unicauca.bancopreguntas.editorial.dobles;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.EstadoProceso;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Doble en memoria de {@link ProcesoDeRevisionRepositorio}; conserva el orden de apertura.
 */
public final class ProcesoDeRevisionRepositorioEnMemoria implements ProcesoDeRevisionRepositorio {

    private final Map<ProcesoDeRevisionId, ProcesoDeRevision> procesos = new LinkedHashMap<>();

    @Override
    public void guardar(ProcesoDeRevision proceso) {
        procesos.put(proceso.getId(), proceso);
    }

    @Override
    public Optional<ProcesoDeRevision> obtenerPorId(ProcesoDeRevisionId procesoId) {
        return Optional.ofNullable(procesos.get(procesoId));
    }

    @Override
    public Optional<ProcesoDeRevision> obtenerVigentePorPregunta(PreguntaId preguntaId) {
        List<ProcesoDeRevision> historicos = buscarHistoricosPorPregunta(preguntaId);
        return historicos.stream()
                .filter(proceso -> proceso.getEstado() == EstadoProceso.ABIERTO)
                .findFirst()
                .or(() -> historicos.isEmpty() ? Optional.empty() : Optional.of(historicos.get(historicos.size() - 1)));
    }

    @Override
    public List<ProcesoDeRevision> buscarHistoricosPorPregunta(PreguntaId preguntaId) {
        return procesos.values().stream().filter(proceso -> proceso.getPreguntaId().equals(preguntaId)).toList();
    }

    @Override
    public List<ProcesoDeRevision> buscarActivosPorRevisor(UsuarioId revisorId) {
        return procesos.values().stream()
                .filter(proceso -> proceso.getEstado() == EstadoProceso.ABIERTO)
                .filter(proceso -> proceso.tieneAsignado(revisorId))
                .toList();
    }

    /**
     * Cuántos Procesos hay guardados.
     *
     * @return cantidad
     */
    public int cantidad() {
        return procesos.size();
    }
}
