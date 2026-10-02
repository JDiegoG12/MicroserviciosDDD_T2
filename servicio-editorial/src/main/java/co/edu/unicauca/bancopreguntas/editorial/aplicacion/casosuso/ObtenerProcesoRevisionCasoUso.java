package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.ProcesoRevisionNoEncontradoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ObtenerProcesoRevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;

/**
 * Consulta de un Proceso de revisión. Roles {@code ADMINISTRADOR} o {@code REVISOR} asignado. Endpoint futuro:
 * {@code GET /procesos-revision/{procesoId}} (200).
 */
public final class ObtenerProcesoRevisionCasoUso implements ObtenerProcesoRevision {

    private final ProcesoDeRevisionRepositorio procesoRepositorio;

    /**
     * Crea el caso de uso.
     *
     * @param procesoRepositorio repositorio de Procesos de revisión
     */
    public ObtenerProcesoRevisionCasoUso(ProcesoDeRevisionRepositorio procesoRepositorio) {
        this.procesoRepositorio = procesoRepositorio;
    }

    /**
     * {@inheritDoc}
     *
     * @throws AccesoDenegadoExcepcion              sin rol válido o si el Revisor no está asignado
     * @throws ProcesoRevisionNoEncontradoExcepcion si el Proceso no existe
     */
    @Override
    public ProcesoRevisionRespuesta ejecutar(UsuarioActual usuario, String procesoId) {
        usuario.exigirAlgunRol(Rol.ADMINISTRADOR, Rol.REVISOR);
        ProcesoDeRevision proceso = procesoRepositorio.obtenerPorId(ProcesoDeRevisionId.de(procesoId))
                .orElseThrow(() -> new ProcesoRevisionNoEncontradoExcepcion(procesoId));
        if (!usuario.tieneRol(Rol.ADMINISTRADOR) && !proceso.tieneAsignado(usuario.id())) {
            throw new AccesoDenegadoExcepcion("Solo un revisor asignado puede consultar el proceso " + procesoId + ".");
        }
        return MapeadorDeResultados.aProcesoRevisionRespuesta(proceso);
    }
}
