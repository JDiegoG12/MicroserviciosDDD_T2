package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.PreguntaNoEncontradaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarTrazabilidad;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.TrazabilidadRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;

/**
 * CU-18, Consultar trazabilidad de una pregunta. Rol {@code ADMINISTRADOR}. Endpoint futuro:
 * {@code GET /preguntas/{preguntaId}/trazabilidad} (200).
 *
 * <p>Devuelve los registros y el Historial de revisiones de todos los Procesos en orden cronológico, en una
 * sola lectura del agregado, porque el Historial vive en la Pregunta (D-15). No altera el estado.</p>
 */
public final class ConsultarTrazabilidadCasoUso implements ConsultarTrazabilidad {

    private final PreguntaRepositorio preguntaRepositorio;

    /**
     * Crea el caso de uso.
     *
     * @param preguntaRepositorio repositorio de Preguntas
     */
    public ConsultarTrazabilidadCasoUso(PreguntaRepositorio preguntaRepositorio) {
        this.preguntaRepositorio = preguntaRepositorio;
    }

    /**
     * {@inheritDoc}
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion sin rol {@code ADMINISTRADOR}
     * @throws PreguntaNoEncontradaExcepcion                                                        si no existe
     */
    @Override
    public TrazabilidadRespuesta ejecutar(UsuarioActual usuario, String preguntaId) {
        usuario.exigirAlgunRol(Rol.ADMINISTRADOR);
        Pregunta pregunta = preguntaRepositorio.obtenerPorId(PreguntaId.de(preguntaId))
                .orElseThrow(() -> new PreguntaNoEncontradaExcepcion(preguntaId));
        return MapeadorDeResultados.aTrazabilidadRespuesta(pregunta);
    }
}
