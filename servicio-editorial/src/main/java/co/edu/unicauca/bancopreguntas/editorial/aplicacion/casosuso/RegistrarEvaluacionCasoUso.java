package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConversorDeComandos;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.RegistrarEvaluacionComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.PreguntaNoEncontradaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.ProcesoRevisionNoEncontradoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.RegistrarEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EventoDeDominio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.DecisionRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.FormatoDeEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.ResolutorDictamenServicio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * CU-11, Diligenciar formato de evaluación, con CU-12 (Emitir dictamen) automático. Rol {@code REVISOR}
 * asignado. Endpoint futuro: {@code POST /procesos-revision/{procesoId}/evaluaciones} (201).
 *
 * <p>Registra el Formato en el Proceso, lo traslada al Historial de la Pregunta (D-15) y, si era la última
 * evaluación pendiente, el Proceso emite su Dictamen y la Pregunta pasa a {@code APROBADA} o vuelve a
 * {@code EN_CONSTRUCCION} (INV-19, INV-20, D-07).</p>
 *
 * <p>DUDA: igual que la asignación, guarda dos agregados en un mismo caso de uso (CONTRATOS.md 3.3.5 frente a
 * D-15 y Taller 1, 10.3). En la etapa 2 ambos guardados deben ir en la misma transacción.</p>
 */
public final class RegistrarEvaluacionCasoUso implements RegistrarEvaluacion {

    private final ProcesoDeRevisionRepositorio procesoRepositorio;
    private final PreguntaRepositorio preguntaRepositorio;
    private final ResolutorDictamenServicio resolutorDictamen;
    private final PublicadorEventosPuerto publicadorEventos;
    private final RelojPuerto reloj;

    /**
     * Crea el caso de uso con sus puertos y el servicio de dominio.
     *
     * @param procesoRepositorio  repositorio de Procesos de revisión
     * @param preguntaRepositorio repositorio de Preguntas
     * @param resolutorDictamen   servicio de dominio de resolución (10.3)
     * @param publicadorEventos   publicador de eventos
     * @param reloj               fuente de la hora
     */
    public RegistrarEvaluacionCasoUso(ProcesoDeRevisionRepositorio procesoRepositorio, PreguntaRepositorio preguntaRepositorio,
                                      ResolutorDictamenServicio resolutorDictamen, PublicadorEventosPuerto publicadorEventos,
                                      RelojPuerto reloj) {
        this.procesoRepositorio = procesoRepositorio;
        this.preguntaRepositorio = preguntaRepositorio;
        this.resolutorDictamen = resolutorDictamen;
        this.publicadorEventos = publicadorEventos;
        this.reloj = reloj;
    }

    /**
     * {@inheritDoc}
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion          sin rol {@code REVISOR}
     * @throws ProcesoRevisionNoEncontradoExcepcion                                                          si el Proceso no existe
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.RevisorNoAsignadoExcepcion       si no está asignado
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.EvaluacionYaRegistradaExcepcion  si ya evaluó (INV-18)
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.ProcesoCerradoExcepcion          si el Proceso está cerrado (INV-21)
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion            si los criterios o la decisión no son válidos
     */
    @Override
    public ProcesoRevisionRespuesta ejecutar(UsuarioActual usuario, RegistrarEvaluacionComando comando) {
        // CONTRATOS 11.1: al registrar la evaluación se exige que X-Roles contenga REVISOR.
        usuario.exigirAlgunRol(Rol.REVISOR);
        ProcesoDeRevision proceso = procesoRepositorio.obtenerPorId(ProcesoDeRevisionId.de(comando.procesoId()))
                .orElseThrow(() -> new ProcesoRevisionNoEncontradoExcepcion(comando.procesoId()));
        Pregunta pregunta = preguntaRepositorio.obtenerPorId(proceso.getPreguntaId())
                .orElseThrow(() -> new PreguntaNoEncontradaExcepcion(proceso.getPreguntaId().toString()));
        Instant ahora = reloj.ahora();

        FormatoDeEvaluacion formato = proceso.registrarEvaluacion(usuario.id(),
                ConversorDeComandos.aCriterios(comando.criterios()),
                ConversorDeComandos.aObservaciones(comando.observaciones()),
                ConversorDeComandos.aEnum(DecisionRevision.class, comando.decision(), "decision"),
                ahora);
        resolutorDictamen.resolver(proceso, formato, pregunta, usuario.id(), ahora);
        procesoRepositorio.guardar(proceso);
        preguntaRepositorio.guardar(pregunta);

        List<EventoDeDominio> eventos = new ArrayList<>(proceso.extraerEventos());
        eventos.addAll(pregunta.extraerEventos());
        publicadorEventos.publicar(eventos);
        return MapeadorDeResultados.aProcesoRevisionRespuesta(proceso);
    }
}
