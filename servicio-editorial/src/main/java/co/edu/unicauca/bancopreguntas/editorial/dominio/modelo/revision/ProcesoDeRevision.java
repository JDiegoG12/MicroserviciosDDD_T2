package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.DictamenEmitido;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EvaluacionRegistrada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.ProcesoDeRevisionAbierto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.RevisoresAsignados;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AutorNoPuedeSerRevisorExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.EvaluacionYaRegistradaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.ProcesoCerradoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.RevisorDuplicadoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.RevisorNoAsignadoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.RevisoresInsuficientesExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.RaizDeAgregado;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Raíz del agregado Proceso de revisión: una instancia concreta de la Revisión por pares sobre una
 * Pregunta (Taller 1, sección 7.2).
 *
 * <p>Su frontera abarca el conjunto completo de Formatos de evaluación, porque el Dictamen solo se
 * calcula cuando todos los Revisores asignados evaluaron (INV-19). Protege INV-15 a INV-21.</p>
 */
public final class ProcesoDeRevision extends RaizDeAgregado {

    /** D-03, INV-15: mínimo de Revisores por Proceso. */
    public static final int MINIMO_REVISORES = 2;

    private final ProcesoDeRevisionId id;
    private final PreguntaId preguntaId;
    private final UsuarioId autorId;
    private final Instant fechaApertura;
    private final List<AsignacionDeRevisor> asignaciones;
    private final List<FormatoDeEvaluacion> formatos;
    private EstadoProceso estado;
    private Dictamen dictamen;

    private ProcesoDeRevision(ProcesoDeRevisionId id, PreguntaId preguntaId, UsuarioId autorId, Instant fechaApertura,
                              List<AsignacionDeRevisor> asignaciones, List<FormatoDeEvaluacion> formatos,
                              EstadoProceso estado, Dictamen dictamen) {
        this.id = Validaciones.requerirNoNulo(id, "procesoId");
        this.preguntaId = Validaciones.requerirNoNulo(preguntaId, "preguntaId");
        this.autorId = Validaciones.requerirNoNulo(autorId, "autorId");
        this.fechaApertura = Validaciones.requerirNoNulo(fechaApertura, "fechaApertura");
        this.asignaciones = new ArrayList<>(Validaciones.requerirNoNulo(asignaciones, "asignaciones"));
        this.formatos = new ArrayList<>(Validaciones.requerirNoNulo(formatos, "formatos"));
        this.estado = Validaciones.requerirNoNulo(estado, "estado");
        this.dictamen = dictamen;
    }

    /**
     * Abre el Proceso con sus Asignaciones de revisor (CU-10, D-14).
     *
     * @param id           identidad nueva
     * @param preguntaId   Pregunta evaluada
     * @param autorId      Autor de la Pregunta (se recibe como dato, no se carga la Pregunta; Taller 1, 10.2)
     * @param revisoresIds Revisores propuestos
     * @param fecha        instante de apertura
     * @return el Proceso abierto, con los eventos {@code ProcesoDeRevisionAbierto} y {@code RevisoresAsignados}
     * @throws RevisoresInsuficientesExcepcion si hay menos de dos Revisores (INV-15, D-03)
     * @throws RevisorDuplicadoExcepcion       si un Revisor se repite (INV-17)
     * @throws AutorNoPuedeSerRevisorExcepcion si el Autor está entre los Revisores (INV-16, D-06)
     */
    public static ProcesoDeRevision abrir(ProcesoDeRevisionId id, PreguntaId preguntaId, UsuarioId autorId,
                                          List<UsuarioId> revisoresIds, Instant fecha) {
        Validaciones.requerirNoNulo(revisoresIds, "revisoresIds");
        revisoresIds.forEach(revisor -> Validaciones.requerirNoNulo(revisor, "revisoresIds[]"));
        Validaciones.requerirNoNulo(fecha, "fecha");
        exigirMinimoDeRevisores(revisoresIds);
        exigirRevisoresSinRepetir(revisoresIds);
        exigirQueElAutorNoSeaRevisor(autorId, revisoresIds);

        List<AsignacionDeRevisor> asignaciones = revisoresIds.stream()
                .map(revisor -> new AsignacionDeRevisor(revisor, fecha))
                .toList();
        ProcesoDeRevision proceso = new ProcesoDeRevision(id, preguntaId, autorId, fecha, asignaciones, List.of(),
                EstadoProceso.ABIERTO, null);
        proceso.registrarEvento(new ProcesoDeRevisionAbierto(id, preguntaId, autorId, fecha));
        proceso.registrarEvento(new RevisoresAsignados(id, revisoresIds, fecha));
        return proceso;
    }

    /**
     * Reconstruye un Proceso ya existente desde la persistencia, sin validar ni emitir eventos (etapa 2).
     *
     * @param id            identidad
     * @param preguntaId    Pregunta evaluada
     * @param autorId       Autor de la Pregunta
     * @param fechaApertura instante de apertura
     * @param asignaciones  Asignaciones de revisor
     * @param formatos      Formatos de evaluación emitidos
     * @param estado        estado guardado
     * @param dictamen      dictamen o {@code null}
     * @return el Proceso reconstruido
     */
    public static ProcesoDeRevision reconstituir(ProcesoDeRevisionId id, PreguntaId preguntaId, UsuarioId autorId,
                                                 Instant fechaApertura, List<AsignacionDeRevisor> asignaciones,
                                                 List<FormatoDeEvaluacion> formatos, EstadoProceso estado,
                                                 Dictamen dictamen) {
        return new ProcesoDeRevision(id, preguntaId, autorId, fechaApertura, asignaciones, formatos, estado, dictamen);
    }

    /**
     * Registra el Formato de evaluación de un Revisor asignado (CU-11). Si era la última evaluación
     * pendiente, calcula el Dictamen y cierra el Proceso en el mismo paso (CU-12, INV-19).
     *
     * @param revisorId     Revisor que evalúa
     * @param criterios     valoraciones de los tres criterios
     * @param observaciones observaciones
     * @param decision      decisión aprobatoria o reprobatoria
     * @param fecha         instante de emisión
     * @return el formato registrado
     * @throws ProcesoCerradoExcepcion          si el Proceso ya tiene Dictamen (INV-21)
     * @throws RevisorNoAsignadoExcepcion       si el usuario no está asignado
     * @throws EvaluacionYaRegistradaExcepcion  si el Revisor ya evaluó (INV-18)
     */
    public FormatoDeEvaluacion registrarEvaluacion(UsuarioId revisorId, List<CriterioEvaluado> criterios,
                                                   List<Observacion> observaciones, DecisionRevision decision,
                                                   Instant fecha) {
        // INV-21: un Dictamen emitido es inmutable y cierra definitivamente el Proceso.
        if (estado == EstadoProceso.CERRADO) {
            throw new ProcesoCerradoExcepcion("El proceso de revisión " + id + " ya está cerrado.");
        }
        if (!tieneAsignado(revisorId)) {
            throw new RevisorNoAsignadoExcepcion("El usuario " + revisorId + " no está asignado a este proceso.");
        }
        // INV-18: cada Revisor produce a lo sumo un Formato de evaluación, inmutable una vez emitido.
        if (yaEvaluo(revisorId)) {
            throw new EvaluacionYaRegistradaExcepcion("El revisor " + revisorId + " ya registró su evaluación.");
        }
        FormatoDeEvaluacion formato = new FormatoDeEvaluacion(FormatoId.generar(), revisorId, criterios, observaciones,
                decision, fecha);
        formatos.add(formato);
        registrarEvento(new EvaluacionRegistrada(id, preguntaId, formato.getId(), revisorId, decision, fecha));
        emitirDictamenSiNoQuedanPendientes(fecha);
        return formato;
    }

    // INV-19: el Dictamen solo se calcula cuando el 100 % de los asignados evaluó.
    private void emitirDictamenSiNoQuedanPendientes(Instant fecha) {
        if (cantidadDeEvaluacionesPendientes() > 0) {
            return;
        }
        int aprobatorias = (int) formatos.stream().filter(FormatoDeEvaluacion::esAprobatorio).count();
        dictamen = Dictamen.calcular(aprobatorias, asignaciones.size(), fecha);
        estado = EstadoProceso.CERRADO;
        registrarEvento(new DictamenEmitido(id, preguntaId, dictamen));
    }

    // INV-15, D-03: mínimo dos Asignaciones de revisor.
    private static void exigirMinimoDeRevisores(List<UsuarioId> revisoresIds) {
        if (revisoresIds.size() < MINIMO_REVISORES) {
            throw new RevisoresInsuficientesExcepcion("Se requieren al menos " + MINIMO_REVISORES
                    + " revisores; se recibieron " + revisoresIds.size() + ".");
        }
    }

    // INV-17: un mismo Revisor no puede tener más de una Asignación dentro del mismo Proceso.
    private static void exigirRevisoresSinRepetir(List<UsuarioId> revisoresIds) {
        Set<UsuarioId> distintos = new HashSet<>(revisoresIds);
        if (distintos.size() != revisoresIds.size()) {
            throw new RevisorDuplicadoExcepcion("Un mismo revisor no puede asignarse dos veces al proceso.");
        }
    }

    // INV-16, D-06: ningún Revisor asignado puede ser el Autor de la Pregunta.
    private static void exigirQueElAutorNoSeaRevisor(UsuarioId autorId, List<UsuarioId> revisoresIds) {
        if (revisoresIds.contains(autorId)) {
            throw new AutorNoPuedeSerRevisorExcepcion("El autor de la pregunta no puede ser su revisor.");
        }
    }

    /**
     * Indica si un usuario tiene una Asignación en este Proceso.
     *
     * @param usuarioId usuario a consultar
     * @return {@code true} si está asignado
     */
    public boolean tieneAsignado(UsuarioId usuarioId) {
        return asignaciones.stream().anyMatch(asignacion -> asignacion.revisorId().equals(usuarioId));
    }

    /**
     * Indica si un Revisor ya registró su evaluación.
     *
     * @param revisorId Revisor a consultar
     * @return {@code true} si ya hay un formato suyo
     */
    public boolean yaEvaluo(UsuarioId revisorId) {
        return formatos.stream().anyMatch(formato -> formato.getRevisorId().equals(revisorId));
    }

    /**
     * Cuenta los Revisores asignados que aún no evalúan.
     *
     * @return cantidad de evaluaciones pendientes
     */
    public int cantidadDeEvaluacionesPendientes() {
        return asignaciones.size() - formatos.size();
    }

    /**
     * Indica si el Proceso cerró con un Dictamen favorable (lo consulta {@code PublicadorPreguntaServicio}).
     *
     * @return {@code true} si está cerrado con resultado {@code APROBADA}
     */
    public boolean tieneDictamenAprobatorio() {
        return estado == EstadoProceso.CERRADO && dictamen != null && dictamen.esAprobatorio();
    }

    /**
     * Devuelve la identidad.
     *
     * @return identificador del Proceso
     */
    public ProcesoDeRevisionId getId() {
        return id;
    }

    /**
     * Devuelve la Pregunta evaluada.
     *
     * @return identificador de la Pregunta
     */
    public PreguntaId getPreguntaId() {
        return preguntaId;
    }

    /**
     * Devuelve el Autor de la Pregunta.
     *
     * @return identificador del Autor
     */
    public UsuarioId getAutorId() {
        return autorId;
    }

    /**
     * Devuelve el instante de apertura.
     *
     * @return fecha UTC
     */
    public Instant getFechaApertura() {
        return fechaApertura;
    }

    /**
     * Devuelve las Asignaciones de revisor.
     *
     * @return lista inmutable
     */
    public List<AsignacionDeRevisor> getAsignaciones() {
        return List.copyOf(asignaciones);
    }

    /**
     * Devuelve los Formatos de evaluación emitidos, en orden de registro.
     *
     * @return lista inmutable
     */
    public List<FormatoDeEvaluacion> getFormatos() {
        return List.copyOf(formatos);
    }

    /**
     * Devuelve el estado del Proceso.
     *
     * @return {@code ABIERTO} o {@code CERRADO}
     */
    public EstadoProceso getEstado() {
        return estado;
    }

    /**
     * Devuelve el Dictamen, si ya se emitió.
     *
     * @return dictamen o vacío
     */
    public Optional<Dictamen> getDictamen() {
        return Optional.ofNullable(dictamen);
    }

    /**
     * Dos Procesos son el mismo si tienen la misma identidad.
     *
     * @param otro objeto a comparar
     * @return {@code true} si comparten identidad
     */
    @Override
    public boolean equals(Object otro) {
        return otro instanceof ProcesoDeRevision proceso && id.equals(proceso.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
