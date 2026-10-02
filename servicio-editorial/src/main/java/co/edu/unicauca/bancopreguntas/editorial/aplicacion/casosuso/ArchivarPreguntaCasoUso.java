package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ArchivarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.PreguntaNoEncontradaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ArchivarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.MotivoDeArchivado;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;

/**
 * CU-09, Archivar pregunta. Rol {@code ADMINISTRADOR}. Endpoint futuro:
 * {@code POST /preguntas/{preguntaId}/archivado} (200, más el evento {@code PreguntaArchivada} con el motivo).
 */
public final class ArchivarPreguntaCasoUso implements ArchivarPregunta {

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
    public ArchivarPreguntaCasoUso(PreguntaRepositorio preguntaRepositorio, PublicadorEventosPuerto publicadorEventos,
                                   RelojPuerto reloj) {
        this.preguntaRepositorio = preguntaRepositorio;
        this.publicadorEventos = publicadorEventos;
        this.reloj = reloj;
    }

    /**
     * {@inheritDoc}
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion        sin rol {@code ADMINISTRADOR}
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion          si falta el motivo o supera 500 caracteres (400)
     * @throws PreguntaNoEncontradaExcepcion                                                               si no existe
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.TransicionNoPermitidaExcepcion si no está {@code PUBLICADA}
     */
    @Override
    public PreguntaRespuesta ejecutar(UsuarioActual usuario, ArchivarPreguntaComando comando) {
        usuario.exigirAlgunRol(Rol.ADMINISTRADOR);
        // CONTRATOS 8.1: 400 si falta el motivo, antes de buscar la Pregunta.
        MotivoDeArchivado motivo = new MotivoDeArchivado(comando.motivo());
        Pregunta pregunta = preguntaRepositorio.obtenerPorId(PreguntaId.de(comando.preguntaId()))
                .orElseThrow(() -> new PreguntaNoEncontradaExcepcion(comando.preguntaId()));

        pregunta.archivar(motivo, usuario.id(), reloj.ahora());
        preguntaRepositorio.guardar(pregunta);
        publicadorEventos.publicar(pregunta.extraerEventos());
        return MapeadorDeResultados.aPreguntaRespuesta(pregunta);
    }
}
