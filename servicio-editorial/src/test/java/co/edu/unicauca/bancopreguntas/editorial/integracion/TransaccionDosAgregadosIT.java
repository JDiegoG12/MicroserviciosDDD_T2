package co.edu.unicauca.bancopreguntas.editorial.integracion;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.AsignarRevisoresCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.AsignarRevisoresComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.AlcanceDeVisibilidad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.CriteriosBusquedaPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.AsignadorRevisoresServicio;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.transaccion.EjecutorTransaccional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Excepción documentada a 3.3.5 (CONTRATOS.md 11.1): {@code AsignarRevisoresCasoUso} guarda el Proceso y la Pregunta
 * en una sola transacción local. Si el guardado de la Pregunta falla, el Proceso tampoco queda guardado.
 */
@DisplayName("Transacción única de dos agregados (excepción a 3.3.5)")
class TransaccionDosAgregadosIT extends PruebaDeIntegracion {

    @Autowired
    PreguntaRepositorio preguntaRepositorio;
    @Autowired
    ProcesoDeRevisionRepositorio procesoRepositorio;
    @Autowired
    PublicadorEventosPuerto publicadorEventos;
    @Autowired
    RelojPuerto reloj;
    @Autowired
    EjecutorTransaccional transaccion;
    @Autowired
    JdbcTemplate jdbc;

    /** Repositorio que lee de verdad pero falla al guardar, después de que el Proceso ya se guardó. */
    private static final class PreguntaRepositorioQueFallaAlGuardar implements PreguntaRepositorio {
        private final PreguntaRepositorio real;

        PreguntaRepositorioQueFallaAlGuardar(PreguntaRepositorio real) {
            this.real = real;
        }

        @Override
        public void guardar(Pregunta pregunta) {
            throw new IllegalStateException("Falla simulada al guardar la pregunta");
        }

        @Override
        public Optional<Pregunta> obtenerPorId(PreguntaId preguntaId) {
            return real.obtenerPorId(preguntaId);
        }

        @Override
        public Pagina<Pregunta> buscarPorCriterios(CriteriosBusquedaPregunta criterios, AlcanceDeVisibilidad alcance,
                                                   Paginacion paginacion) {
            return real.buscarPorCriterios(criterios, alcance, paginacion);
        }

        @Override
        public List<Pregunta> buscarPorIds(Collection<PreguntaId> preguntaIds) {
            return real.buscarPorIds(preguntaIds);
        }
    }

    @Test
    @DisplayName("Si falla el guardado de la pregunta, el proceso tampoco queda guardado y la pregunta sigue PENDIENTE_REVISION")
    void rollbackDeLosDosAgregados() {
        String preguntaId = crearPendiente(usuarioNuevo(Rol.AUTOR), UUID.randomUUID());
        AsignarRevisoresCasoUso casoDeUso = new AsignarRevisoresCasoUso(new PreguntaRepositorioQueFallaAlGuardar(preguntaRepositorio),
                procesoRepositorio, new AsignadorRevisoresServicio(), publicadorEventos, reloj);
        AsignarRevisoresComando comando = new AsignarRevisoresComando(preguntaId,
                List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        assertThatThrownBy(() -> transaccion.escribir(() -> casoDeUso.ejecutar(ADMINISTRADOR, comando)))
                .hasMessageContaining("Falla simulada");

        Integer procesos = jdbc.queryForObject("select count(*) from proceso_revision where pregunta_id = ?", Integer.class,
                UUID.fromString(preguntaId));
        assertThat(procesos).isZero();
        Pregunta pregunta = transaccion.leer(() -> preguntaRepositorio.obtenerPorId(PreguntaId.de(preguntaId)).orElseThrow());
        assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
    }

    @Test
    @DisplayName("En el caso feliz los dos agregados quedan guardados en la misma transacción")
    void casoFelizGuardaLosDos() {
        String preguntaId = crearPendiente(usuarioNuevo(Rol.AUTOR), UUID.randomUUID());

        asignar(preguntaId, usuarioNuevo(Rol.REVISOR), usuarioNuevo(Rol.REVISOR));

        Integer procesos = jdbc.queryForObject("select count(*) from proceso_revision where pregunta_id = ?", Integer.class,
                UUID.fromString(preguntaId));
        String estado = jdbc.queryForObject("select estado from pregunta where id = ?", String.class, UUID.fromString(preguntaId));
        assertThat(procesos).isEqualTo(1);
        assertThat(estado).isEqualTo("EN_REVISION");
    }
}
