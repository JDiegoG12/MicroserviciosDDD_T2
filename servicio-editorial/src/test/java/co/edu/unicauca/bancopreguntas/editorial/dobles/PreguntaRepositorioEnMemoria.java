package co.edu.unicauca.bancopreguntas.editorial.dobles;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.CriteriosBusquedaPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Doble en memoria de {@link PreguntaRepositorio} para las pruebas de aplicación.
 */
public final class PreguntaRepositorioEnMemoria implements PreguntaRepositorio {

    private final Map<PreguntaId, Pregunta> preguntas = new LinkedHashMap<>();
    private int cantidadDeGuardados;

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
    public Pagina<Pregunta> buscarPorCriterios(CriteriosBusquedaPregunta criterios, Paginacion paginacion) {
        List<Pregunta> filtradas = preguntas.values().stream()
                .filter(criterios::seCumplenEn)
                .sorted(Comparator.comparing(Pregunta::getFechaCreacion))
                .toList();
        return Pagina.desdeLista(filtradas, paginacion);
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
