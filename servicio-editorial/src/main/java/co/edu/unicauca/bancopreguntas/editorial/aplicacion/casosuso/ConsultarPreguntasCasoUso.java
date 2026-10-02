package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarPreguntasConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConversorDeComandos;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarPreguntas;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaResumen;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.CriteriosBusquedaPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.NivelDeDificultad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * CU-06, Consultar preguntas mediante filtros. Endpoint futuro: {@code GET /preguntas} (200, página).
 *
 * <p>Restricción por rol (CU-06, RNF-07): {@code ADMINISTRADOR} ve todas, {@code AUTOR} solo las suyas,
 * {@code REVISOR} las de sus Procesos abiertos y {@code DOCENTE} solo las {@code PUBLICADA}.</p>
 *
 * <p>DUDA: CONTRATOS.md no dice qué ocurre si un usuario tiene varios de esos roles. Se aplica el rol más
 * amplio en este orden: ADMINISTRADOR, AUTOR, REVISOR, DOCENTE. Un usuario sin ninguno de ellos (por ejemplo,
 * solo ESTUDIANTE) recibe ACCESO_DENEGADO.</p>
 */
public final class ConsultarPreguntasCasoUso implements ConsultarPreguntas {

    private final PreguntaRepositorio preguntaRepositorio;
    private final ProcesoDeRevisionRepositorio procesoRepositorio;

    /**
     * Crea el caso de uso con sus repositorios.
     *
     * @param preguntaRepositorio repositorio de Preguntas
     * @param procesoRepositorio  repositorio de Procesos de revisión
     */
    public ConsultarPreguntasCasoUso(PreguntaRepositorio preguntaRepositorio, ProcesoDeRevisionRepositorio procesoRepositorio) {
        this.preguntaRepositorio = preguntaRepositorio;
        this.procesoRepositorio = procesoRepositorio;
    }

    /**
     * {@inheritDoc}
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion si no tiene un rol que consulte preguntas
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion   si un filtro o la paginación no son válidos
     */
    @Override
    public Pagina<PreguntaResumen> ejecutar(UsuarioActual usuario, ConsultarPreguntasConsulta consulta) {
        usuario.exigirAlgunRol(Rol.ADMINISTRADOR, Rol.AUTOR, Rol.REVISOR, Rol.DOCENTE);
        CriteriosBusquedaPregunta criterios = aCriterios(consulta);
        Paginacion paginacion = Paginacion.de(consulta.pagina(), consulta.tamano());

        Pagina<Pregunta> pagina;
        if (usuario.tieneRol(Rol.ADMINISTRADOR)) {
            pagina = preguntaRepositorio.buscarPorCriterios(criterios, paginacion);
        } else if (usuario.tieneRol(Rol.AUTOR)) {
            pagina = consultarComoAutor(usuario.id(), criterios, paginacion);
        } else if (usuario.tieneRol(Rol.REVISOR)) {
            pagina = consultarComoRevisor(usuario.id(), criterios, paginacion);
        } else {
            pagina = consultarComoDocente(criterios, paginacion);
        }
        return pagina.mapear(MapeadorDeResultados::aPreguntaResumen);
    }

    // CU-06: un Autor consulta solo sus propias Preguntas.
    private Pagina<Pregunta> consultarComoAutor(UsuarioId autor, CriteriosBusquedaPregunta criterios, Paginacion paginacion) {
        if (criterios.autorId() != null && !criterios.autorId().equals(autor)) {
            return Pagina.desdeLista(List.of(), paginacion);
        }
        return preguntaRepositorio.buscarPorCriterios(criterios.conAutor(autor), paginacion);
    }

    // CU-06: un Docente consulta solo las Preguntas PUBLICADA.
    private Pagina<Pregunta> consultarComoDocente(CriteriosBusquedaPregunta criterios, Paginacion paginacion) {
        if (criterios.estado() != null && criterios.estado() != EstadoPregunta.PUBLICADA) {
            return Pagina.desdeLista(List.of(), paginacion);
        }
        return preguntaRepositorio.buscarPorCriterios(criterios.conEstado(EstadoPregunta.PUBLICADA), paginacion);
    }

    // CU-06 y Taller 1, 12.1: un Revisor consulta las Preguntas de sus Procesos activos (dos repositorios).
    private Pagina<Pregunta> consultarComoRevisor(UsuarioId revisor, CriteriosBusquedaPregunta criterios, Paginacion paginacion) {
        List<PreguntaId> asignadas = procesoRepositorio.buscarActivosPorRevisor(revisor).stream()
                .map(ProcesoDeRevision::getPreguntaId)
                .distinct()
                .toList();
        List<Pregunta> filtradas = preguntaRepositorio.buscarPorIds(asignadas).stream()
                .filter(criterios::seCumplenEn)
                .sorted(Comparator.comparing(Pregunta::getFechaCreacion))
                .toList();
        return Pagina.desdeLista(filtradas, paginacion);
    }

    private static CriteriosBusquedaPregunta aCriterios(ConsultarPreguntasConsulta consulta) {
        Validaciones.requerirNoNulo(consulta, "consulta");
        return new CriteriosBusquedaPregunta(
                uuidOpcional(consulta.competenciaId(), "competenciaId"),
                uuidOpcional(consulta.temaId(), "temaId"),
                uuidOpcional(consulta.subtemaId(), "subtemaId"),
                ConversorDeComandos.aEnumOpcional(NivelDeDificultad.class, consulta.nivelDificultad(), "nivelDificultad"),
                ConversorDeComandos.aEnumOpcional(EstadoPregunta.class, consulta.estado(), "estado"),
                consulta.autorId() == null ? null : UsuarioId.de(consulta.autorId()));
    }

    private static UUID uuidOpcional(String texto, String campo) {
        return texto == null ? null : Validaciones.aUuid(texto, campo);
    }
}
