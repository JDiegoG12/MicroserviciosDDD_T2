package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.AsignarRevisoresComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.PreguntaNoEncontradaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.AsignarRevisores;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EventoDeDominio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.AsignadorRevisoresServicio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * CU-10, Asignar revisores. Rol {@code ADMINISTRADOR}. Endpoint futuro:
 * {@code POST /preguntas/{preguntaId}/procesos-revision} (201).
 *
 * <p><strong>Excepción documentada a la regla 3.3.5</strong> (CONTRATOS.md 3.3.5 y 11.1, "Excepción a 3.3.5"):
 * este caso de uso guarda el {@code ProcesoDeRevision} nuevo y la {@code Pregunta} en <strong>una sola
 * transacción local</strong>, con bloqueo optimista en ambos agregados. La transacción la abre la
 * infraestructura, que envuelve el caso de uso completo. Justificación:</p>
 * <ul>
 *   <li>D-14 define la apertura del Proceso y el paso de la Pregunta a {@code EN_REVISION} como un solo hecho del
 *       negocio, y el Taller 1 (10.2) asigna esa coordinación a {@code AsignadorRevisoresServicio};</li>
 *   <li>los dos agregados viven en el mismo contexto y la misma base de datos, así que la consistencia eventual
 *       con eventos internos agregaría complejidad sin beneficio.</li>
 * </ul>
 */
public final class AsignarRevisoresCasoUso implements AsignarRevisores {

    private final PreguntaRepositorio preguntaRepositorio;
    private final ProcesoDeRevisionRepositorio procesoRepositorio;
    private final AsignadorRevisoresServicio asignadorRevisores;
    private final PublicadorEventosPuerto publicadorEventos;
    private final RelojPuerto reloj;

    /**
     * Crea el caso de uso con sus puertos y el servicio de dominio.
     *
     * @param preguntaRepositorio repositorio de Preguntas
     * @param procesoRepositorio  repositorio de Procesos de revisión
     * @param asignadorRevisores  servicio de dominio de asignación (10.2)
     * @param publicadorEventos   publicador de eventos
     * @param reloj               fuente de la hora
     */
    public AsignarRevisoresCasoUso(PreguntaRepositorio preguntaRepositorio, ProcesoDeRevisionRepositorio procesoRepositorio,
                                   AsignadorRevisoresServicio asignadorRevisores, PublicadorEventosPuerto publicadorEventos,
                                   RelojPuerto reloj) {
        this.preguntaRepositorio = preguntaRepositorio;
        this.procesoRepositorio = procesoRepositorio;
        this.asignadorRevisores = asignadorRevisores;
        this.publicadorEventos = publicadorEventos;
        this.reloj = reloj;
    }

    /**
     * {@inheritDoc}
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion         sin rol {@code ADMINISTRADOR}
     * @throws PreguntaNoEncontradaExcepcion                                                                si la Pregunta no existe
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.RevisoresInsuficientesExcepcion menos de dos Revisores (INV-15)
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AutorNoPuedeSerRevisorExcepcion el Autor es Revisor (INV-16)
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.RevisorDuplicadoExcepcion       Revisor repetido (INV-17)
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.TransicionNoPermitidaExcepcion  la Pregunta no está {@code PENDIENTE_REVISION}
     */
    @Override
    public ProcesoRevisionRespuesta ejecutar(UsuarioActual usuario, AsignarRevisoresComando comando) {
        usuario.exigirAlgunRol(Rol.ADMINISTRADOR);
        Pregunta pregunta = preguntaRepositorio.obtenerPorId(PreguntaId.de(comando.preguntaId()))
                .orElseThrow(() -> new PreguntaNoEncontradaExcepcion(comando.preguntaId()));
        List<UsuarioId> revisores = Validaciones.requerirNoNulo(comando.revisoresIds(), "revisoresIds").stream()
                .map(UsuarioId::de)
                .toList();
        Instant ahora = reloj.ahora();

        ProcesoDeRevision proceso = asignadorRevisores.asignar(pregunta, ProcesoDeRevisionId.generar(), revisores,
                usuario.id(), ahora);
        procesoRepositorio.guardar(proceso);
        preguntaRepositorio.guardar(pregunta);

        List<EventoDeDominio> eventos = new ArrayList<>(proceso.extraerEventos());
        eventos.addAll(pregunta.extraerEventos());
        publicadorEventos.publicar(eventos);
        return MapeadorDeResultados.aProcesoRevisionRespuesta(proceso);
    }
}
