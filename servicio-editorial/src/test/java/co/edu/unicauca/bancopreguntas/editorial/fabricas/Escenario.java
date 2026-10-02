package co.edu.unicauca.bancopreguntas.editorial.fabricas;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.ArchivarPreguntaCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.AsignarRevisoresCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.ConsultarPreguntasCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.ConsultarProcesosRevisionCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.ConsultarTrazabilidadCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.CrearPreguntaCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.EnviarPreguntaARevisionCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.ModificarPreguntaCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.ObtenerPreguntaCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.ObtenerProcesoRevisionCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.PublicarPreguntaCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.RegistrarEvaluacionCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.AsignarRevisoresComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.DatosDePreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.RegistrarEvaluacionComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dobles.CatalogoAcademicoFalso;
import co.edu.unicauca.bancopreguntas.editorial.dobles.PreguntaRepositorioEnMemoria;
import co.edu.unicauca.bancopreguntas.editorial.dobles.ProcesoDeRevisionRepositorioEnMemoria;
import co.edu.unicauca.bancopreguntas.editorial.dobles.PublicadorEventosEnMemoria;
import co.edu.unicauca.bancopreguntas.editorial.dobles.RelojFijo;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.AsignadorRevisoresServicio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.PublicadorPreguntaServicio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.ResolutorDictamenServicio;

import java.util.List;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.*;

/**
 * Escenario de pruebas de aplicación: los doce casos de uso cableados con dobles en memoria, los usuarios
 * de CONTRATOS.md 4.2 y atajos para llevar una Pregunta por su ciclo de vida.
 */
public final class Escenario {

    /** Administrador de prueba. */
    public static final UsuarioActual COMO_ADMINISTRADOR = UsuarioActual.de(ADMINISTRADOR, Rol.ADMINISTRADOR);
    /** Autor de prueba. */
    public static final UsuarioActual COMO_AUTOR = UsuarioActual.de(AUTOR, Rol.AUTOR);
    /** Otro autor (se usa el id del Revisor 3 con rol AUTOR). */
    public static final UsuarioActual COMO_OTRO_AUTOR = UsuarioActual.de(REVISOR_3, Rol.AUTOR);
    /** Revisor 1 de prueba. */
    public static final UsuarioActual COMO_REVISOR_1 = UsuarioActual.de(REVISOR_1, Rol.REVISOR);
    /** Revisor 2 de prueba. */
    public static final UsuarioActual COMO_REVISOR_2 = UsuarioActual.de(REVISOR_2, Rol.REVISOR);
    /** Revisor 3 de prueba. */
    public static final UsuarioActual COMO_REVISOR_3 = UsuarioActual.de(REVISOR_3, Rol.REVISOR);
    /** Docente de prueba. */
    public static final UsuarioActual COMO_DOCENTE = UsuarioActual.de(DOCENTE, Rol.DOCENTE);
    /** Estudiante de prueba. */
    public static final UsuarioActual COMO_ESTUDIANTE = UsuarioActual.de(ESTUDIANTE, Rol.ESTUDIANTE);

    /** Repositorio de Preguntas en memoria. */
    public final PreguntaRepositorioEnMemoria preguntas = new PreguntaRepositorioEnMemoria();
    /** Repositorio de Procesos en memoria. */
    public final ProcesoDeRevisionRepositorioEnMemoria procesos = new ProcesoDeRevisionRepositorioEnMemoria();
    /** Catálogo falso. */
    public final CatalogoAcademicoFalso catalogo = new CatalogoAcademicoFalso();
    /** Publicador de eventos en memoria. */
    public final PublicadorEventosEnMemoria publicador = new PublicadorEventosEnMemoria();
    /** Reloj fijo. */
    public final RelojFijo reloj = new RelojFijo(FECHA);

    /** CU-04. */
    public final CrearPreguntaCasoUso crearPregunta = new CrearPreguntaCasoUso(preguntas, catalogo, publicador, reloj);
    /** CU-05. */
    public final ModificarPreguntaCasoUso modificarPregunta = new ModificarPreguntaCasoUso(preguntas, catalogo, publicador, reloj);
    /** CU-06. */
    public final ConsultarPreguntasCasoUso consultarPreguntas = new ConsultarPreguntasCasoUso(preguntas, procesos);
    /** CU-06 (una). */
    public final ObtenerPreguntaCasoUso obtenerPregunta = new ObtenerPreguntaCasoUso(preguntas, procesos);
    /** CU-07. */
    public final EnviarPreguntaARevisionCasoUso enviarARevision = new EnviarPreguntaARevisionCasoUso(preguntas, publicador, reloj);
    /** CU-10. */
    public final AsignarRevisoresCasoUso asignarRevisores = new AsignarRevisoresCasoUso(preguntas, procesos,
            new AsignadorRevisoresServicio(), publicador, reloj);
    /** CU-11 + CU-12. */
    public final RegistrarEvaluacionCasoUso registrarEvaluacion = new RegistrarEvaluacionCasoUso(procesos, preguntas,
            new ResolutorDictamenServicio(), publicador, reloj);
    /** Consulta de un proceso. */
    public final ObtenerProcesoRevisionCasoUso obtenerProceso = new ObtenerProcesoRevisionCasoUso(procesos);
    /** CU-06 (procesos). */
    public final ConsultarProcesosRevisionCasoUso consultarProcesos = new ConsultarProcesosRevisionCasoUso(procesos);
    /** CU-08. */
    public final PublicarPreguntaCasoUso publicarPregunta = new PublicarPreguntaCasoUso(preguntas, procesos,
            new PublicadorPreguntaServicio(), publicador, reloj);
    /** CU-09. */
    public final ArchivarPreguntaCasoUso archivarPregunta = new ArchivarPreguntaCasoUso(preguntas, publicador, reloj);
    /** CU-18. */
    public final ConsultarTrazabilidadCasoUso consultarTrazabilidad = new ConsultarTrazabilidadCasoUso(preguntas);

    /**
     * Datos de una Pregunta completa (la de CONTRATOS.md 7.4).
     *
     * @return datos válidos
     */
    public static DatosDePreguntaComando datosValidos() {
        return datosCon("Un grupo de 5 estudiantes obtuvo las notas 3,0; 3,5; 4,0; 4,0 y 4,5.");
    }

    /**
     * Datos de una Pregunta completa con el contexto indicado.
     *
     * @param contexto contexto
     * @return datos
     */
    public static DatosDePreguntaComando datosCon(String contexto) {
        return new DatosDePreguntaComando(
                contexto,
                "¿Cuál es la moda del conjunto de notas?",
                List.of(new DatosDePreguntaComando.OpcionComando("A", "3,0", false),
                        new DatosDePreguntaComando.OpcionComando("B", "3,8", false),
                        new DatosDePreguntaComando.OpcionComando("C", "4,0", true),
                        new DatosDePreguntaComando.OpcionComando("D", "4,5", false)),
                "La moda es el valor que más se repite: 4,0 aparece dos veces.",
                List.of("Walpole, R. Probabilidad y estadística. Pearson, 2012."),
                "22222222-2222-4222-8222-000000000101",
                "22222222-2222-4222-8222-000000000201",
                "22222222-2222-4222-8222-000000000301",
                "BAJO");
    }

    /**
     * Datos de una Pregunta incompleta (solo clasificación y nivel), que queda en {@code BORRADOR}.
     *
     * @return datos incompletos
     */
    public static DatosDePreguntaComando datosIncompletos() {
        return new DatosDePreguntaComando(null, null, null, null, null,
                "22222222-2222-4222-8222-000000000101",
                "22222222-2222-4222-8222-000000000201",
                "22222222-2222-4222-8222-000000000301",
                "MEDIO");
    }

    /**
     * Evaluación con los tres criterios y la decisión indicada.
     *
     * @param procesoId UUID del Proceso
     * @param decision  {@code APROBATORIA} o {@code REPROBATORIA}
     * @return comando de evaluación
     */
    public static RegistrarEvaluacionComando evaluacion(String procesoId, String decision) {
        return new RegistrarEvaluacionComando(procesoId,
                List.of(new RegistrarEvaluacionComando.CriterioComando("PEDAGOGICO", 4),
                        new RegistrarEvaluacionComando.CriterioComando("TECNICO", 5),
                        new RegistrarEvaluacionComando.CriterioComando("ESTRUCTURAL", 4)),
                List.of("El distractor B es poco plausible"),
                decision);
    }

    /**
     * Crea una Pregunta completa como el Autor ({@code EN_CONSTRUCCION}).
     *
     * @return UUID de la Pregunta
     */
    public String crearPreguntaCompleta() {
        String preguntaId = crearPregunta.ejecutar(COMO_AUTOR, datosValidos()).preguntaId();
        reloj.avanzarMinutos(1);
        return preguntaId;
    }

    /**
     * Lleva una Pregunta nueva hasta {@code PENDIENTE_REVISION}.
     *
     * @return UUID de la Pregunta
     */
    public String crearPreguntaPendiente() {
        String preguntaId = crearPreguntaCompleta();
        enviarARevision.ejecutar(COMO_AUTOR, preguntaId);
        reloj.avanzarMinutos(1);
        return preguntaId;
    }

    /**
     * Asigna los Revisores 1 y 2 a una Pregunta pendiente.
     *
     * @param preguntaId UUID de la Pregunta
     * @return UUID del Proceso abierto
     */
    public String asignarDosRevisores(String preguntaId) {
        ProcesoRevisionRespuesta proceso = asignarRevisores.ejecutar(COMO_ADMINISTRADOR,
                new AsignarRevisoresComando(preguntaId, List.of(REVISOR_1.toString(), REVISOR_2.toString())));
        reloj.avanzarMinutos(1);
        return proceso.procesoId();
    }

    /**
     * Ambos Revisores evalúan con las decisiones indicadas.
     *
     * @param procesoId UUID del Proceso
     * @param decision1 decisión del Revisor 1
     * @param decision2 decisión del Revisor 2
     */
    public void evaluarPorAmbos(String procesoId, String decision1, String decision2) {
        registrarEvaluacion.ejecutar(COMO_REVISOR_1, evaluacion(procesoId, decision1));
        reloj.avanzarMinutos(1);
        registrarEvaluacion.ejecutar(COMO_REVISOR_2, evaluacion(procesoId, decision2));
        reloj.avanzarMinutos(1);
    }

    /**
     * Lleva una Pregunta nueva hasta {@code APROBADA}.
     *
     * @return UUID de la Pregunta
     */
    public String crearPreguntaAprobada() {
        String preguntaId = crearPreguntaPendiente();
        evaluarPorAmbos(asignarDosRevisores(preguntaId), "APROBATORIA", "APROBATORIA");
        return preguntaId;
    }

    /**
     * Lleva una Pregunta nueva hasta {@code PUBLICADA}.
     *
     * @return UUID de la Pregunta
     */
    public String crearPreguntaPublicada() {
        String preguntaId = crearPreguntaAprobada();
        publicarPregunta.ejecutar(COMO_ADMINISTRADOR, preguntaId);
        reloj.avanzarMinutos(1);
        return preguntaId;
    }
}
