package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.PreguntaNoEncontradaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.EnviarPreguntaARevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;

/**
 * CU-07, Someter pregunta a revisión. Rol {@code AUTOR} (dueño). Endpoint futuro:
 * {@code POST /preguntas/{preguntaId}/envio-revision} (200).
 */
public final class EnviarPreguntaARevisionCasoUso implements EnviarPreguntaARevision {

    private final PreguntaRepositorio preguntaRepositorio;
    private final PublicadorEventosPuerto publicadorEventos;
    private final RelojPuerto reloj;

    /**
     * Crea el caso de uso con sus puertos.
     *
     * @param preguntaRepositorio repositorio de Preguntas
     * @param publicadorEventos   publicador de eventos
     * @param reloj               fuente de la hora
     */
    public EnviarPreguntaARevisionCasoUso(PreguntaRepositorio preguntaRepositorio,
                                          PublicadorEventosPuerto publicadorEventos, RelojPuerto reloj) {
        this.preguntaRepositorio = preguntaRepositorio;
        this.publicadorEventos = publicadorEventos;
        this.reloj = reloj;
    }

    /**
     * {@inheritDoc}
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion        sin rol {@code AUTOR} o si no es el autor
     * @throws PreguntaNoEncontradaExcepcion                                                               si no existe
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.TransicionNoPermitidaExcepcion si no está {@code EN_CONSTRUCCION}
     */
    @Override
    public PreguntaRespuesta ejecutar(UsuarioActual usuario, String preguntaId) {
        usuario.exigirAlgunRol(Rol.AUTOR);
        Pregunta pregunta = preguntaRepositorio.obtenerPorId(PreguntaId.de(preguntaId))
                .orElseThrow(() -> new PreguntaNoEncontradaExcepcion(preguntaId));

        pregunta.enviarARevision(usuario.id(), reloj.ahora());
        preguntaRepositorio.guardar(pregunta);
        publicadorEventos.publicar(pregunta.extraerEventos());
        return MapeadorDeResultados.aPreguntaRespuesta(pregunta);
    }
}
