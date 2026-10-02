package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarProcesosConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConversorDeComandos;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarProcesosRevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.EstadoProceso;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;

import java.util.Comparator;
import java.util.List;

/**
 * CU-06 para Revisores: Procesos abiertos de un Revisor. Roles {@code REVISOR} o {@code ADMINISTRADOR}.
 * Endpoint futuro: {@code GET /procesos-revision?revisorId={uuid}&estado=ABIERTO} (200, página).
 *
 * <p>DUDA: el repositorio del Taller 1 (12.2) solo ofrece {@code buscarActivosPorRevisor}, así que:
 * (1) {@code estado} admite solo {@code ABIERTO} (o ausente); (2) un REVISOR que no es ADMINISTRADOR solo
 * consulta sus propios procesos; (3) {@code revisorId} es obligatorio para el ADMINISTRADOR.</p>
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
     * @throws DatoInvalidoExcepcion   si el estado no es {@code ABIERTO}, falta el revisor o la paginación es inválida
     */
    @Override
    public Pagina<ProcesoRevisionRespuesta> ejecutar(UsuarioActual usuario, ConsultarProcesosConsulta consulta) {
        usuario.exigirAlgunRol(Rol.REVISOR, Rol.ADMINISTRADOR);
        exigirEstadoAbierto(consulta.estado());
        UsuarioId revisor = determinarRevisor(usuario, consulta.revisorId());
        Paginacion paginacion = Paginacion.de(consulta.pagina(), consulta.tamano());

        List<ProcesoDeRevision> procesos = procesoRepositorio.buscarActivosPorRevisor(revisor).stream()
                .sorted(Comparator.comparing(ProcesoDeRevision::getFechaApertura))
                .toList();
        return Pagina.desdeLista(procesos, paginacion).mapear(MapeadorDeResultados::aProcesoRevisionRespuesta);
    }

    private static void exigirEstadoAbierto(String estado) {
        EstadoProceso solicitado = ConversorDeComandos.aEnumOpcional(EstadoProceso.class, estado, "estado");
        if (solicitado != null && solicitado != EstadoProceso.ABIERTO) {
            throw new DatoInvalidoExcepcion("Solo se pueden consultar procesos en estado ABIERTO.");
        }
    }

    private static UsuarioId determinarRevisor(UsuarioActual usuario, String revisorIdSolicitado) {
        if (usuario.tieneRol(Rol.ADMINISTRADOR)) {
            if (revisorIdSolicitado == null) {
                throw new DatoInvalidoExcepcion("El parámetro revisorId es obligatorio.");
            }
            return UsuarioId.de(revisorIdSolicitado);
        }
        if (revisorIdSolicitado != null && !UsuarioId.de(revisorIdSolicitado).equals(usuario.id())) {
            throw new AccesoDenegadoExcepcion("Un revisor solo puede consultar sus propios procesos.");
        }
        return usuario.id();
    }
}
