package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarProcesosConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConversorDeComandos;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarProcesosRevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.EstadoProceso;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;

/**
 * CU-06 para Revisores: Procesos de revisión por estado y revisor. Roles {@code REVISOR} o {@code ADMINISTRADOR}.
 * Endpoint: {@code GET /procesos-revision?revisorId={uuid}&estado={ABIERTO|CERRADO}} (200, página).
 *
 * <p>Filtros de CONTRATOS.md 8.1:</p>
 * <ul>
 *   <li>{@code estado} es opcional: {@code ABIERTO} por defecto o {@code CERRADO}; otro valor → 400;</li>
 *   <li>un {@code REVISOR} sin rol {@code ADMINISTRADOR} solo ve sus procesos: si omite {@code revisorId} se usa
 *       el suyo, y si pide el de otro → 403;</li>
 *   <li>el {@code ADMINISTRADOR} puede omitir {@code revisorId} para ver todos los procesos de ese estado.</li>
 * </ul>
 */
public final class ConsultarProcesosRevisionCasoUso implements ConsultarProcesosRevision {

    private final ProcesoDeRevisionRepositorio procesoRepositorio;

    /**
     * Crea el caso de uso.
     *
     * @param procesoRepositorio repositorio de Procesos de revisión
     */
    public ConsultarProcesosRevisionCasoUso(ProcesoDeRevisionRepositorio procesoRepositorio) {
        this.procesoRepositorio = procesoRepositorio;
    }

    /**
     * {@inheritDoc}
     *
     * @throws AccesoDenegadoExcepcion sin rol válido, o si un Revisor consulta los procesos de otro
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si el estado, el
     *         revisor o la paginación no son válidos
     */
    @Override
    public Pagina<ProcesoRevisionRespuesta> ejecutar(UsuarioActual usuario, ConsultarProcesosConsulta consulta) {
        usuario.exigirAlgunRol(Rol.REVISOR, Rol.ADMINISTRADOR);
        EstadoProceso estado = estadoSolicitado(consulta.estado());
        UsuarioId revisor = determinarRevisor(usuario, consulta.revisorId());
        Paginacion paginacion = Paginacion.de(consulta.pagina(), consulta.tamano());

        return procesoRepositorio.buscarPorEstadoYRevisor(estado, revisor, paginacion)
                .mapear(MapeadorDeResultados::aProcesoRevisionRespuesta);
    }

    // CONTRATOS.md 8.1: ABIERTO por defecto, o CERRADO; otro valor → 400 SOLICITUD_INVALIDA.
    private static EstadoProceso estadoSolicitado(String estado) {
        EstadoProceso solicitado = ConversorDeComandos.aEnumOpcional(EstadoProceso.class, estado, "estado");
        return solicitado == null ? EstadoProceso.ABIERTO : solicitado;
    }

    // CONTRATOS.md 8.1: el ADMINISTRADOR puede omitir revisorId; el REVISOR solo consulta los suyos.
    private static UsuarioId determinarRevisor(UsuarioActual usuario, String revisorIdSolicitado) {
        UsuarioId solicitado = revisorIdSolicitado == null ? null : UsuarioId.de(revisorIdSolicitado);
        if (usuario.tieneRol(Rol.ADMINISTRADOR)) {
            return solicitado;
        }
        if (solicitado != null && !solicitado.equals(usuario.id())) {
            throw new AccesoDenegadoExcepcion("Un revisor solo puede consultar sus propios procesos.");
        }
        return usuario.id();
    }
}
