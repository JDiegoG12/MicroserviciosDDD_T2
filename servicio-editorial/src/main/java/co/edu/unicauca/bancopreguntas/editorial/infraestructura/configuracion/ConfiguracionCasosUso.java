package co.edu.unicauca.bancopreguntas.editorial.infraestructura.configuracion;

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
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ArchivarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.AsignarRevisores;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarPreguntas;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarProcesosRevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarTrazabilidad;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.CrearPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.EnviarPreguntaARevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ModificarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ObtenerPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ObtenerProcesoRevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.PublicarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.RegistrarEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.CatalogoAcademicoPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.AsignadorRevisoresServicio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.PublicadorPreguntaServicio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.ResolutorDictamenServicio;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.transaccion.EjecutorTransaccional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara los doce casos de uso como beans de sus puertos de entrada (Etapa 2, "Capas y Spring"). La capa de
 * aplicación no tiene anotaciones de Spring: aquí se crea cada caso de uso con sus puertos y se envuelve en el
 * {@link EjecutorTransaccional}, de modo que <strong>todo el caso de uso</strong> corre en una transacción.
 *
 * <p>Los casos de uso que guardan dos agregados (excepción a 3.3.5, CONTRATOS.md 11.1) quedan así en una sola
 * transacción local, y los eventos se publican después del commit (7.7.1).</p>
 */
@Configuration
public class ConfiguracionCasosUso {

    /**
     * CU-04.
     *
     * @param preguntas  repositorio de Preguntas
     * @param catalogo   puerto de Catálogo
     * @param publicador publicador de eventos
     * @param reloj      reloj
     * @param tx         decorador transaccional
     * @return puerto de entrada transaccional
     */
    @Bean
    public CrearPregunta crearPregunta(PreguntaRepositorio preguntas, CatalogoAcademicoPuerto catalogo,
                                       PublicadorEventosPuerto publicador, RelojPuerto reloj, EjecutorTransaccional tx) {
        CrearPregunta casoDeUso = new CrearPreguntaCasoUso(preguntas, catalogo, publicador, reloj);
        return (usuario, datos) -> tx.escribir(() -> casoDeUso.ejecutar(usuario, datos));
    }

    /**
     * CU-05.
     *
     * @param preguntas  repositorio de Preguntas
     * @param catalogo   puerto de Catálogo
     * @param publicador publicador de eventos
     * @param reloj      reloj
     * @param tx         decorador transaccional
     * @return puerto de entrada transaccional
     */
    @Bean
    public ModificarPregunta modificarPregunta(PreguntaRepositorio preguntas, CatalogoAcademicoPuerto catalogo,
                                               PublicadorEventosPuerto publicador, RelojPuerto reloj, EjecutorTransaccional tx) {
        ModificarPregunta casoDeUso = new ModificarPreguntaCasoUso(preguntas, catalogo, publicador, reloj);
        return (usuario, comando) -> tx.escribir(() -> casoDeUso.ejecutar(usuario, comando));
    }

    /**
     * CU-06.
     *
     * @param preguntas repositorio de Preguntas
     * @param tx        decorador transaccional
     * @return puerto de entrada transaccional (solo lectura)
     */
    @Bean
    public ConsultarPreguntas consultarPreguntas(PreguntaRepositorio preguntas, EjecutorTransaccional tx) {
        ConsultarPreguntas casoDeUso = new ConsultarPreguntasCasoUso(preguntas);
        return (usuario, consulta) -> tx.leer(() -> casoDeUso.ejecutar(usuario, consulta));
    }

    /**
     * CU-06 para una Pregunta.
     *
     * @param preguntas repositorio de Preguntas
     * @param procesos  repositorio de Procesos
     * @param tx        decorador transaccional
     * @return puerto de entrada transaccional (solo lectura)
     */
    @Bean
    public ObtenerPregunta obtenerPregunta(PreguntaRepositorio preguntas, ProcesoDeRevisionRepositorio procesos,
                                           EjecutorTransaccional tx) {
        ObtenerPregunta casoDeUso = new ObtenerPreguntaCasoUso(preguntas, procesos);
        return (usuario, preguntaId) -> tx.leer(() -> casoDeUso.ejecutar(usuario, preguntaId));
    }

    /**
     * CU-07.
     *
     * @param preguntas  repositorio de Preguntas
     * @param publicador publicador de eventos
     * @param reloj      reloj
     * @param tx         decorador transaccional
     * @return puerto de entrada transaccional
     */
    @Bean
    public EnviarPreguntaARevision enviarPreguntaARevision(PreguntaRepositorio preguntas, PublicadorEventosPuerto publicador,
                                                           RelojPuerto reloj, EjecutorTransaccional tx) {
        EnviarPreguntaARevision casoDeUso = new EnviarPreguntaARevisionCasoUso(preguntas, publicador, reloj);
        return (usuario, preguntaId) -> tx.escribir(() -> casoDeUso.ejecutar(usuario, preguntaId));
    }

    /**
     * CU-10 (excepción a 3.3.5: dos agregados en una transacción).
     *
     * @param preguntas  repositorio de Preguntas
     * @param procesos   repositorio de Procesos
     * @param publicador publicador de eventos
     * @param reloj      reloj
     * @param tx         decorador transaccional
     * @return puerto de entrada transaccional
     */
    @Bean
    public AsignarRevisores asignarRevisores(PreguntaRepositorio preguntas, ProcesoDeRevisionRepositorio procesos,
                                             PublicadorEventosPuerto publicador, RelojPuerto reloj, EjecutorTransaccional tx) {
        AsignarRevisores casoDeUso = new AsignarRevisoresCasoUso(preguntas, procesos, new AsignadorRevisoresServicio(),
                publicador, reloj);
        return (usuario, comando) -> tx.escribir(() -> casoDeUso.ejecutar(usuario, comando));
    }

    /**
     * CU-11 + CU-12 (excepción a 3.3.5: dos agregados en una transacción).
     *
     * @param preguntas  repositorio de Preguntas
     * @param procesos   repositorio de Procesos
     * @param publicador publicador de eventos
     * @param reloj      reloj
     * @param tx         decorador transaccional
     * @return puerto de entrada transaccional
     */
    @Bean
    public RegistrarEvaluacion registrarEvaluacion(PreguntaRepositorio preguntas, ProcesoDeRevisionRepositorio procesos,
                                                   PublicadorEventosPuerto publicador, RelojPuerto reloj,
                                                   EjecutorTransaccional tx) {
        RegistrarEvaluacion casoDeUso = new RegistrarEvaluacionCasoUso(procesos, preguntas, new ResolutorDictamenServicio(),
                publicador, reloj);
        return (usuario, comando) -> tx.escribir(() -> casoDeUso.ejecutar(usuario, comando));
    }

    /**
     * Consulta de un Proceso.
     *
     * @param procesos repositorio de Procesos
     * @param tx       decorador transaccional
     * @return puerto de entrada transaccional (solo lectura)
     */
    @Bean
    public ObtenerProcesoRevision obtenerProcesoRevision(ProcesoDeRevisionRepositorio procesos, EjecutorTransaccional tx) {
        ObtenerProcesoRevision casoDeUso = new ObtenerProcesoRevisionCasoUso(procesos);
        return (usuario, procesoId) -> tx.leer(() -> casoDeUso.ejecutar(usuario, procesoId));
    }

    /**
     * CU-06 para Revisores.
     *
     * @param procesos repositorio de Procesos
     * @param tx       decorador transaccional
     * @return puerto de entrada transaccional (solo lectura)
     */
    @Bean
    public ConsultarProcesosRevision consultarProcesosRevision(ProcesoDeRevisionRepositorio procesos, EjecutorTransaccional tx) {
        ConsultarProcesosRevision casoDeUso = new ConsultarProcesosRevisionCasoUso(procesos);
        return (usuario, consulta) -> tx.leer(() -> casoDeUso.ejecutar(usuario, consulta));
    }

    /**
     * CU-08.
     *
     * @param preguntas  repositorio de Preguntas
     * @param procesos   repositorio de Procesos (solo lectura)
     * @param publicador publicador de eventos
     * @param reloj      reloj
     * @param tx         decorador transaccional
     * @return puerto de entrada transaccional
     */
    @Bean
    public PublicarPregunta publicarPregunta(PreguntaRepositorio preguntas, ProcesoDeRevisionRepositorio procesos,
                                             PublicadorEventosPuerto publicador, RelojPuerto reloj, EjecutorTransaccional tx) {
        PublicarPregunta casoDeUso = new PublicarPreguntaCasoUso(preguntas, procesos, new PublicadorPreguntaServicio(),
                publicador, reloj);
        return (usuario, preguntaId) -> tx.escribir(() -> casoDeUso.ejecutar(usuario, preguntaId));
    }

    /**
     * CU-09.
     *
     * @param preguntas  repositorio de Preguntas
     * @param publicador publicador de eventos
     * @param reloj      reloj
     * @param tx         decorador transaccional
     * @return puerto de entrada transaccional
     */
    @Bean
    public ArchivarPregunta archivarPregunta(PreguntaRepositorio preguntas, PublicadorEventosPuerto publicador,
                                             RelojPuerto reloj, EjecutorTransaccional tx) {
        ArchivarPregunta casoDeUso = new ArchivarPreguntaCasoUso(preguntas, publicador, reloj);
        return (usuario, comando) -> tx.escribir(() -> casoDeUso.ejecutar(usuario, comando));
    }

    /**
     * CU-18.
     *
     * @param preguntas repositorio de Preguntas
     * @param tx        decorador transaccional
     * @return puerto de entrada transaccional (solo lectura)
     */
    @Bean
    public ConsultarTrazabilidad consultarTrazabilidad(PreguntaRepositorio preguntas, EjecutorTransaccional tx) {
        ConsultarTrazabilidad casoDeUso = new ConsultarTrazabilidadCasoUso(preguntas);
        return (usuario, preguntaId) -> tx.leer(() -> casoDeUso.ejecutar(usuario, preguntaId));
    }
}
