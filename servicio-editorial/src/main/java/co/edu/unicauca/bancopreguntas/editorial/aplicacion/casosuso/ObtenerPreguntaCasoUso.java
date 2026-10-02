package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.PreguntaNoEncontradaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ObtenerPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;

/**
 * CU-06 para una sola Pregunta. Cualquier rol, según la restricción de CU-06. Endpoint futuro:
 * {@code GET /preguntas/{preguntaId}} (200).
 *
 * <p>Basta con que alguno de los roles del usuario le permita verla: {@code ADMINISTRADOR} siempre,
 * {@code AUTOR} si es suya, {@code REVISOR} si está asignado a su Proceso vigente y {@code DOCENTE} si está
 * {@code PUBLICADA}.</p>
 *
 * <p>DUDA: CONTRATOS 8.1 solo lista 404 para este endpoint. Para una Pregunta existente que el rol no
 * puede ver se responde ACCESO_DENEGADO (403) en lugar de ocultar su existencia con un 404.</p>
 */
public final class ObtenerPreguntaCasoUso implements ObtenerPregunta {

    private final PreguntaRepositorio preguntaRepositorio;
    private final ProcesoDeRevisionRepositorio procesoRepositorio;

    /**
     * Crea el caso de uso con sus repositorios.
     *
     * @param preguntaRepositorio repositorio de Preguntas
     * @param procesoRepositorio  repositorio de Procesos de revisión
     */
    public ObtenerPreguntaCasoUso(PreguntaRepositorio preguntaRepositorio, ProcesoDeRevisionRepositorio procesoRepositorio) {
        this.preguntaRepositorio = preguntaRepositorio;
        this.procesoRepositorio = procesoRepositorio;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PreguntaNoEncontradaExcepcion si la Pregunta no existe
     * @throws AccesoDenegadoExcepcion       si ningún rol del usuario le permite verla
     */
    @Override
    public PreguntaRespuesta ejecutar(UsuarioActual usuario, String preguntaId) {
        Pregunta pregunta = preguntaRepositorio.obtenerPorId(PreguntaId.de(preguntaId))
                .orElseThrow(() -> new PreguntaNoEncontradaExcepcion(preguntaId));
        if (!puedeVer(usuario, pregunta)) {
            throw new AccesoDenegadoExcepcion("El usuario no tiene permiso para ver la pregunta " + preguntaId + ".");
        }
        return MapeadorDeResultados.aPreguntaRespuesta(pregunta);
    }

    // CU-06, RNF-07: la visibilidad depende del rol.
    private boolean puedeVer(UsuarioActual usuario, Pregunta pregunta) {
        return usuario.tieneRol(Rol.ADMINISTRADOR)
                || (usuario.tieneRol(Rol.AUTOR) && pregunta.esAutor(usuario.id()))
                || (usuario.tieneRol(Rol.DOCENTE) && pregunta.getEstado() == EstadoPregunta.PUBLICADA)
                || (usuario.tieneRol(Rol.REVISOR) && estaAsignado(usuario, pregunta));
    }

    private boolean estaAsignado(UsuarioActual usuario, Pregunta pregunta) {
        return procesoRepositorio.obtenerVigentePorPregunta(pregunta.getId())
                .map(proceso -> proceso.tieneAsignado(usuario.id()))
                .orElse(false);
    }
}
