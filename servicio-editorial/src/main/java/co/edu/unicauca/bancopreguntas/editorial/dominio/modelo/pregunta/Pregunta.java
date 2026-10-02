package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaAprobada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaArchivada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaCreada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaEnRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaModificada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaPublicada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaRechazada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaSometidaARevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.ValidacionEstructuralSuperada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.PreguntaNoEditableExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.TransicionNoPermitidaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.RaizDeAgregado;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.ErrorDeValidacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.ValidacionEstructural;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Raíz del agregado Pregunta: la unidad de consistencia del Banco de preguntas (Taller 1, sección 7.1).
 *
 * <p>Reúne la estructura del ítem y su ciclo de vida de ocho estados. La máquina de estados vive aquí
 * dentro (INV-09): cada método del lenguaje ubicuo comprueba sus precondiciones, consulta la tabla
 * {@link TransicionesDePregunta}, anexa un {@link RegistroDeTrazabilidad} (INV-13) y anota su evento de
 * dominio. La fecha siempre llega como parámetro: el dominio nunca consulta el reloj del sistema.</p>
 *
 * <p>No existe ningún método para borrar una Pregunta (INV-12, RNF-16).</p>
 */
public final class Pregunta extends RaizDeAgregado {

    private final PreguntaId id;
    private final UsuarioId autorId;
    private final Instant fechaCreacion;
    private final List<RegistroDeTrazabilidad> trazabilidad;
    private ContenidoDePregunta contenido;
    private EstadoPregunta estado;
    private HistorialDeRevisiones historialRevisiones;
    private Instant fechaActualizacion;

    private Pregunta(PreguntaId id, UsuarioId autorId, ContenidoDePregunta contenido, EstadoPregunta estado,
                     List<RegistroDeTrazabilidad> trazabilidad, HistorialDeRevisiones historialRevisiones,
                     Instant fechaCreacion, Instant fechaActualizacion) {
        this.id = Validaciones.requerirNoNulo(id, "preguntaId");
        this.autorId = Validaciones.requerirNoNulo(autorId, "autorId");
        this.contenido = Validaciones.requerirNoNulo(contenido, "contenido");
        this.estado = Validaciones.requerirNoNulo(estado, "estado");
        this.trazabilidad = new ArrayList<>(Validaciones.requerirNoNulo(trazabilidad, "trazabilidad"));
        this.historialRevisiones = Validaciones.requerirNoNulo(historialRevisiones, "historialRevisiones");
        this.fechaCreacion = Validaciones.requerirNoNulo(fechaCreacion, "fechaCreacion");
        this.fechaActualizacion = Validaciones.requerirNoNulo(fechaActualizacion, "fechaActualizacion");
    }

    /**
     * Crea una Pregunta (CU-04). Nace en {@code BORRADOR} y, en el mismo paso, se ejecuta la validación
     * estructural: si la supera pasa a {@code EN_CONSTRUCCION} (CONTRATOS.md 8.1, aclaración "Estado al
     * crear"; D-02, INV-11). La trazabilidad registra la {@code CREACION} y, si aplica, la {@code TRANSICION}.
     *
     * @param id        identidad nueva
     * @param autorId   Autor que la crea
     * @param contenido componentes (pueden venir incompletos, D-02)
     * @param fecha     instante de la creación
     * @return la Pregunta creada, con los eventos {@code PreguntaCreada} y, si supera la validación,
     *         {@code ValidacionEstructuralSuperada}
     */
    public static Pregunta crear(PreguntaId id, UsuarioId autorId, ContenidoDePregunta contenido, Instant fecha) {
        Validaciones.requerirNoNulo(fecha, "fecha");
        Pregunta pregunta = new Pregunta(id, autorId, contenido, EstadoPregunta.BORRADOR, List.of(),
                HistorialDeRevisiones.vacio(), fecha, fecha);
        pregunta.anexarRegistro(fecha, autorId, TipoDeRegistro.CREACION, null, EstadoPregunta.BORRADOR,
                "Creación de la pregunta.");
        pregunta.registrarEvento(new PreguntaCreada(id, autorId, fecha));
        pregunta.promoverSiSuperaValidacion(autorId, fecha);
        return pregunta;
    }

    /**
     * Reconstruye una Pregunta ya existente desde la persistencia, sin validar transiciones ni emitir eventos.
     * La usa el mapper de infraestructura (etapa 2).
     *
     * @param id                  identidad
     * @param autorId             Autor
     * @param contenido           componentes
     * @param estado              estado guardado
     * @param trazabilidad        registros en orden cronológico
     * @param historialRevisiones historial de revisiones
     * @param fechaCreacion       instante de creación
     * @param fechaActualizacion  instante del último cambio
     * @return la Pregunta reconstruida
     */
    public static Pregunta reconstituir(PreguntaId id, UsuarioId autorId, ContenidoDePregunta contenido,
                                        EstadoPregunta estado, List<RegistroDeTrazabilidad> trazabilidad,
                                        HistorialDeRevisiones historialRevisiones, Instant fechaCreacion,
                                        Instant fechaActualizacion) {
        return new Pregunta(id, autorId, contenido, estado, trazabilidad, historialRevisiones, fechaCreacion,
                fechaActualizacion);
    }

    /**
     * Modifica el contenido (CU-05). Solo el Autor puede hacerlo y solo en estados editables. Después
     * revalida y ajusta el estado: una Pregunta incompleta vuelve a {@code BORRADOR} y una completa pasa
     * a {@code EN_CONSTRUCCION} (D-02, INV-11).
     *
     * @param usuarioId      quien modifica
     * @param nuevoContenido componentes nuevos
     * @param fecha          instante de la modificación
     * @throws AccesoDenegadoExcepcion      si el usuario no es el Autor ({@code ACCESO_DENEGADO})
     * @throws PreguntaNoEditableExcepcion  si el estado no es editable ({@code PREGUNTA_NO_EDITABLE}, INV-10)
     */
    public void modificar(UsuarioId usuarioId, ContenidoDePregunta nuevoContenido, Instant fecha) {
        exigirQuePuedaModificar(usuarioId);
        this.contenido = Validaciones.requerirNoNulo(nuevoContenido, "contenido");
        anexarRegistro(fecha, usuarioId, TipoDeRegistro.MODIFICACION, estado, estado, "Modificación del contenido.");
        if (estado == EstadoPregunta.BORRADOR) {
            promoverSiSuperaValidacion(usuarioId, fecha);
        } else if (!ValidacionEstructural.esSuperada(contenido)) {
            transitarA(EstadoPregunta.BORRADOR, usuarioId, fecha,
                    "La modificación dejó la pregunta incompleta (D-02).");
        }
        registrarEvento(new PreguntaModificada(id, usuarioId, estado, fecha));
    }

    /**
     * Comprueba las precondiciones de {@link #modificar}: que el usuario sea el Autor y que el estado sea
     * editable. El caso de uso la invoca antes de consultar a Catálogo.
     *
     * @param usuarioId quien quiere modificar
     * @throws AccesoDenegadoExcepcion     si el usuario no es el Autor ({@code ACCESO_DENEGADO})
     * @throws PreguntaNoEditableExcepcion si el estado no es editable ({@code PREGUNTA_NO_EDITABLE}, INV-10)
     */
    public void exigirQuePuedaModificar(UsuarioId usuarioId) {
        exigirQueSeaElAutor(usuarioId, "modificar");
        // INV-10: solo es modificable mientras esté en BORRADOR o EN_CONSTRUCCION (RF-06, D-02).
        if (!estado.esEditable()) {
            throw new PreguntaNoEditableExcepcion("La pregunta está en " + estado
                    + " y solo se puede modificar en BORRADOR o EN_CONSTRUCCION.");
        }
    }

    /**
     * Somete la Pregunta a revisión (CU-07): pasa de {@code EN_CONSTRUCCION} a {@code PENDIENTE_REVISION}.
     *
     * @param usuarioId quien la somete
     * @param fecha     instante del envío
     * @throws AccesoDenegadoExcepcion        si el usuario no es el Autor
     * @throws TransicionNoPermitidaExcepcion si no está en {@code EN_CONSTRUCCION} (D-02)
     */
    public void enviarARevision(UsuarioId usuarioId, Instant fecha) {
        exigirQueSeaElAutor(usuarioId, "enviar a revisión");
        transitarA(EstadoPregunta.PENDIENTE_REVISION, usuarioId, fecha, "Envío a revisión (CU-07).");
        registrarEvento(new PreguntaSometidaARevision(id, usuarioId, fecha));
    }

    /**
     * Pasa a {@code EN_REVISION} porque se abrió su Proceso de revisión (CU-10). La invoca
     * {@code AsignadorRevisoresServicio} (D-14).
     *
     * @param administradorId Administrador que asignó los Revisores
     * @param fecha           instante de la asignación
     * @throws TransicionNoPermitidaExcepcion si no está en {@code PENDIENTE_REVISION}
     */
    public void iniciarRevision(UsuarioId administradorId, Instant fecha) {
        transitarA(EstadoPregunta.EN_REVISION, administradorId, fecha, "Inicio de la revisión por pares (CU-10).");
        registrarEvento(new PreguntaEnRevision(id, administradorId, fecha));
    }

    /**
     * Anexa una evaluación o un dictamen al Historial de revisiones (D-15, INV-14). La invoca
     * {@code ResolutorDictamenServicio}.
     *
     * @param entrada evaluación o dictamen del Proceso vigente
     * @throws TransicionNoPermitidaExcepcion si la Pregunta no está {@code EN_REVISION}
     */
    public void registrarEnHistorial(EntradaDeHistorial entrada) {
        // DUDA: CONTRATOS no fija el código para anexar al historial fuera de EN_REVISION; se usa
        // TRANSICION_NO_PERMITIDA porque es el estado actual el que impide la operación (409).
        if (estado != EstadoPregunta.EN_REVISION) {
            throw new TransicionNoPermitidaExcepcion("Solo se anexan evaluaciones al historial de una pregunta "
                    + "EN_REVISION; esta está en " + estado + ".");
        }
        historialRevisiones = historialRevisiones.anexar(entrada);
    }

    /**
     * Aprueba la Pregunta porque el Dictamen superó el 70 % (CU-12, INV-20).
     *
     * @param usuarioId usuario cuya acción disparó el dictamen
     * @param fecha     instante del dictamen
     * @throws TransicionNoPermitidaExcepcion si no está {@code EN_REVISION}
     */
    public void aprobar(UsuarioId usuarioId, Instant fecha) {
        transitarA(EstadoPregunta.APROBADA, usuarioId, fecha, "Dictamen aprobatorio (CU-12).");
        registrarEvento(new PreguntaAprobada(id, fecha));
    }

    /**
     * Rechaza la Pregunta: pasa por {@code RECHAZADA}, que queda en la trazabilidad, y en el mismo paso
     * vuelve a {@code EN_CONSTRUCCION} conservando íntegro su historial (CU-12, D-07, INV-14).
     *
     * @param usuarioId usuario cuya acción disparó el dictamen
     * @param fecha     instante del dictamen
     * @throws TransicionNoPermitidaExcepcion si no está {@code EN_REVISION}
     */
    public void rechazar(UsuarioId usuarioId, Instant fecha) {
        transitarA(EstadoPregunta.RECHAZADA, usuarioId, fecha, "Dictamen no aprobatorio (CU-12).");
        transitarA(EstadoPregunta.EN_CONSTRUCCION, usuarioId, fecha,
                "Retorno automático a construcción tras el rechazo (D-07).");
        registrarEvento(new PreguntaRechazada(id, fecha));
    }

    /**
     * Publica la Pregunta (CU-08): de {@code APROBADA} a {@code PUBLICADA}. Emite {@code PreguntaPublicada}
     * con todos los datos de CONTRATOS.md 7.4. Que exista un dictamen favorable lo verifica
     * {@code PublicadorPreguntaServicio}.
     *
     * @param administradorId Administrador que publica
     * @param fecha           instante de la publicación
     * @throws TransicionNoPermitidaExcepcion si no está {@code APROBADA} (RF-15)
     */
    public void publicar(UsuarioId administradorId, Instant fecha) {
        transitarA(EstadoPregunta.PUBLICADA, administradorId, fecha, "Publicación (CU-08).");
        registrarEvento(crearEventoPublicacion(fecha));
    }

    /**
     * Archiva la Pregunta (CU-09): de {@code PUBLICADA} a {@code ARCHIVADA}, sin borrarla (INV-12). Conserva
     * su trazabilidad y su historial. Emite {@code PreguntaArchivada} con el motivo (CONTRATOS.md 7.5).
     *
     * @param motivo          motivo obligatorio de 1 a 500 caracteres
     * @param administradorId Administrador que archiva
     * @param fecha           instante del archivado
     * @throws TransicionNoPermitidaExcepcion si no está {@code PUBLICADA}
     */
    public void archivar(MotivoDeArchivado motivo, UsuarioId administradorId, Instant fecha) {
        Validaciones.requerirNoNulo(motivo, "motivo");
        transitarA(EstadoPregunta.ARCHIVADA, administradorId, fecha, "Archivado (CU-09): " + motivo.texto());
        registrarEvento(new PreguntaArchivada(id, motivo.texto(), fecha));
    }

    /**
     * Calcula las reglas de la validación estructural que el contenido actual incumple (RF-08 a RF-13, INV-04).
     *
     * @return reglas incumplidas; vacía si la supera
     */
    public List<ErrorDeValidacion> erroresValidacion() {
        return ValidacionEstructural.validar(contenido);
    }

    /**
     * Indica si un usuario es el Autor de la Pregunta.
     *
     * @param usuarioId usuario a comparar
     * @return {@code true} si es el Autor
     */
    public boolean esAutor(UsuarioId usuarioId) {
        return autorId.equals(usuarioId);
    }

    // D-02, INV-11: solo sale de BORRADOR si supera íntegramente la validación estructural.
    private void promoverSiSuperaValidacion(UsuarioId usuarioId, Instant fecha) {
        if (ValidacionEstructural.esSuperada(contenido)) {
            transitarA(EstadoPregunta.EN_CONSTRUCCION, usuarioId, fecha, "Supera la validación estructural (D-02).");
            registrarEvento(new ValidacionEstructuralSuperada(id, usuarioId, fecha));
        }
    }

    // INV-09: el estado solo cambia por una transición declarada en la tabla; INV-13: toda transición se anota.
    private void transitarA(EstadoPregunta destino, UsuarioId usuarioId, Instant fecha, String detalle) {
        if (!TransicionesDePregunta.estaPermitida(estado, destino)) {
            throw new TransicionNoPermitidaExcepcion(
                    "La pregunta está en " + estado + " y no puede pasar a " + destino + ".");
        }
        EstadoPregunta anterior = estado;
        estado = destino;
        anexarRegistro(fecha, usuarioId, TipoDeRegistro.TRANSICION, anterior, destino, detalle);
    }

    // INV-13, RF-29, RF-30: la trazabilidad es de solo anexado, con fecha y usuario responsable.
    private void anexarRegistro(Instant fecha, UsuarioId usuarioId, TipoDeRegistro tipo, EstadoPregunta anterior,
                                EstadoPregunta nuevo, String detalle) {
        trazabilidad.add(new RegistroDeTrazabilidad(fecha, usuarioId, tipo, anterior, nuevo, detalle));
        fechaActualizacion = fecha;
    }

    private void exigirQueSeaElAutor(UsuarioId usuarioId, String accion) {
        if (!esAutor(usuarioId)) {
            throw new AccesoDenegadoExcepcion("Solo el autor de la pregunta puede " + accion + " la pregunta.");
        }
    }

    private PreguntaPublicada crearEventoPublicacion(Instant fecha) {
        List<PreguntaPublicada.OpcionPublicada> opciones = contenido.opciones().stream()
                .map(opcion -> new PreguntaPublicada.OpcionPublicada(opcion.letra(), opcion.texto()))
                .toList();
        String letraCorrecta = contenido.respuestaCorrecta()
                .map(OpcionDeRespuesta::letra)
                .orElseThrow(() -> new IllegalStateException("Una pregunta aprobada tiene exactamente una respuesta correcta."));
        return new PreguntaPublicada(id, contenido.contexto().texto(), contenido.preguntaDirecta().texto(), opciones,
                letraCorrecta, contenido.clasificacion(), contenido.nivelDificultad(), fecha);
    }

    /**
     * Devuelve la identidad.
     *
     * @return identificador de la Pregunta
     */
    public PreguntaId getId() {
        return id;
    }

    /**
     * Devuelve el Autor.
     *
     * @return identificador del Autor
     */
    public UsuarioId getAutorId() {
        return autorId;
    }

    /**
     * Devuelve el contenido actual.
     *
     * @return componentes de la Pregunta
     */
    public ContenidoDePregunta getContenido() {
        return contenido;
    }

    /**
     * Devuelve el estado del ciclo de vida.
     *
     * @return estado actual
     */
    public EstadoPregunta getEstado() {
        return estado;
    }

    /**
     * Devuelve la trazabilidad en orden cronológico (INV-13). La copia es inmutable.
     *
     * @return registros de trazabilidad
     */
    public List<RegistroDeTrazabilidad> getTrazabilidad() {
        return List.copyOf(trazabilidad);
    }

    /**
     * Devuelve el Historial de revisiones (INV-14).
     *
     * @return historial inmutable
     */
    public HistorialDeRevisiones getHistorialRevisiones() {
        return historialRevisiones;
    }

    /**
     * Devuelve el instante de creación.
     *
     * @return fecha UTC
     */
    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Devuelve el instante del último cambio de contenido o de estado.
     *
     * @return fecha UTC
     */
    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    /**
     * Dos Preguntas son la misma si tienen la misma identidad.
     *
     * @param otro objeto a comparar
     * @return {@code true} si comparten identidad
     */
    @Override
    public boolean equals(Object otro) {
        return otro instanceof Pregunta pregunta && id.equals(pregunta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
