package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.PreguntaNoEncontradaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.PublicarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.PublicadorPreguntaServicio;

/**
 * CU-08, Publicar pregunta. Rol {@code ADMINISTRADOR}. Endpoint futuro:
 * {@code POST /preguntas/{preguntaId}/publicacion} (200, más el evento {@code PreguntaPublicada}).
 *
 * <p>Lee el Proceso vigente (solo lectura) y modifica solo la Pregunta.</p>
 */
public final class PublicarPreguntaCasoUso implements PublicarPregunta {

    private final PreguntaRepositorio preguntaRepositorio;
    private final ProcesoDeRevisionRepositorio procesoRepositorio;
    private final PublicadorPreguntaServicio publicadorPregunta;
    private final PublicadorEventosPuerto publicadorEventos;
    private final RelojPuerto reloj;

    /**
     * Crea el caso de uso con sus puertos y el servicio de dominio.
     *
     * @param preguntaRepositorio repositorio de Preguntas
     * @param procesoRepositorio  repositorio de Procesos de revisión
     * @param publicadorPregunta  servicio de dominio de publicación (10.4)
     * @param publicadorEventos   publicador de eventos
     * @param reloj               fuente de la hora
     */
    public PublicarPreguntaCasoUso(PreguntaRepositorio preguntaRepositorio, ProcesoDeRevisionRepositorio procesoRepositorio,
                                   PublicadorPreguntaServicio publicadorPregunta, PublicadorEventosPuerto publicadorEventos,
                                   RelojPuerto reloj) {
        this.preguntaRepositorio = preguntaRepositorio;
        this.procesoRepositorio = procesoRepositorio;
        this.publicadorPregunta = publicadorPregunta;
        this.publicadorEventos = publicadorEventos;
        this.reloj = reloj;
    }

    /**
     * {@inheritDoc}
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion        sin rol {@code ADMINISTRADOR}
     * @throws PreguntaNoEncontradaExcepcion                                                               si no existe
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.TransicionNoPermitidaExcepcion si no está {@code APROBADA} o no hay dictamen favorable
     */
    @Override
    public PreguntaRespuesta ejecutar(UsuarioActual usuario, String preguntaId) {
        usuario.exigirAlgunRol(Rol.ADMINISTRADOR);
        Pregunta pregunta = preguntaRepositorio.obtenerPorId(PreguntaId.de(preguntaId))
                .orElseThrow(() -> new PreguntaNoEncontradaExcepcion(preguntaId));

        publicadorPregunta.publicar(pregunta, procesoRepositorio.obtenerVigentePorPregunta(pregunta.getId()),
                usuario.id(), reloj.ahora());
        preguntaRepositorio.guardar(pregunta);
        publicadorEventos.publicar(pregunta.extraerEventos());
        return MapeadorDeResultados.aPreguntaRespuesta(pregunta);
    }
}
