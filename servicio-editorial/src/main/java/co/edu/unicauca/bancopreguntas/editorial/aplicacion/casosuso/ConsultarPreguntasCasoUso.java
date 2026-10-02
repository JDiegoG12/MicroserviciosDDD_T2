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
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.AlcanceDeVisibilidad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.CriteriosBusquedaPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.NivelDeDificultad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;

import java.util.UUID;

/**
 * CU-06, Consultar preguntas mediante filtros. Endpoint: {@code GET /preguntas} (200, página de
 * {@code PreguntaResumen}).
 *
 * <p>Restricción por rol (CU-06, RNF-07; CONTRATOS.md 8.1): {@code ADMINISTRADOR} ve todas; si no, se ve la
 * <strong>unión</strong> de lo que permite cada rol del usuario (D-08): {@code AUTOR} sus preguntas,
 * {@code REVISOR} las de sus Procesos abiertos y {@code DOCENTE} las {@code PUBLICADA}. Los filtros y la
 * paginación se aplican sobre esa unión en la base de datos. Un usuario que solo tiene {@code ESTUDIANTE}
 * recibe 403 {@code ACCESO_DENEGADO}.</p>
 */
public final class ConsultarPreguntasCasoUso implements ConsultarPreguntas {

    private final PreguntaRepositorio preguntaRepositorio;

    /**
     * Crea el caso de uso.
     *
     * @param preguntaRepositorio repositorio de Preguntas
     */
    public ConsultarPreguntasCasoUso(PreguntaRepositorio preguntaRepositorio) {
        this.preguntaRepositorio = preguntaRepositorio;
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

        return preguntaRepositorio.buscarPorCriterios(criterios, alcanceDe(usuario), paginacion)
                .mapear(MapeadorDeResultados::aPreguntaResumen);
    }

    // CONTRATOS.md 8.1 y D-08: ADMINISTRADOR ve todas; si no, la unión de lo que permite cada rol.
    private static AlcanceDeVisibilidad alcanceDe(UsuarioActual usuario) {
        if (usuario.tieneRol(Rol.ADMINISTRADOR)) {
            return AlcanceDeVisibilidad.todas();
        }
        AlcanceDeVisibilidad alcance = AlcanceDeVisibilidad.ninguna();
        if (usuario.tieneRol(Rol.AUTOR)) {
            alcance = alcance.conPropiasDe(usuario.id());
        }
        if (usuario.tieneRol(Rol.REVISOR)) {
            alcance = alcance.conAsignadasA(usuario.id());
        }
        if (usuario.tieneRol(Rol.DOCENTE)) {
            alcance = alcance.conPublicadas();
        }
        return alcance;
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
