package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Bibliografia;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ClasificacionAcademica;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ContenidoDePregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Contexto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.DictamenEnHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EntradaDeHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EvaluacionEnHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.HistorialDeRevisiones;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Justificacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.OpcionDeRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaDirecta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.RegistroDeTrazabilidad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.CriterioEvaluado;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Dictamen;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Observacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.CriterioEmbebido;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.EntradaHistorialEntidad;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.OpcionEmbebida;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.PreguntaEntidad;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.RegistroTrazabilidadEmbebido;

import java.util.List;

/**
 * Traduce el agregado Pregunta a su modelo de base de datos y viceversa (CONTRATOS.md 3.3.2).
 *
 * <p>La trazabilidad y el historial se copian <strong>solo anexando</strong> las entradas nuevas: nunca se
 * reescriben ni se borran filas existentes (INV-13, INV-14).</p>
 */
public final class MapeadorPreguntaJpa {

    private MapeadorPreguntaJpa() {
    }

    /**
     * Reconstruye el agregado desde la base de datos.
     *
     * @param entidad modelo de base de datos
     * @return la Pregunta reconstituida
     */
    public static Pregunta aDominio(PreguntaEntidad entidad) {
        ContenidoDePregunta contenido = new ContenidoDePregunta(
                new Contexto(entidad.getContexto()),
                new PreguntaDirecta(entidad.getPreguntaDirecta()),
                entidad.getOpciones().stream()
                        .map(opcion -> new OpcionDeRespuesta(opcion.getLetra(), opcion.getTexto(), opcion.isEsCorrecta()))
                        .toList(),
                new Justificacion(entidad.getJustificacion()),
                new Bibliografia(List.copyOf(entidad.getBibliografia())),
                new ClasificacionAcademica(entidad.getCompetenciaId(), entidad.getTemaId(), entidad.getSubtemaId()),
                entidad.getNivelDificultad());
        List<RegistroDeTrazabilidad> trazabilidad = entidad.getTrazabilidad().stream()
                .map(registro -> new RegistroDeTrazabilidad(registro.getFecha(), new UsuarioId(registro.getUsuarioId()),
                        registro.getTipo(), registro.getEstadoAnterior(), registro.getEstadoNuevo(), registro.getDetalle()))
                .toList();
        HistorialDeRevisiones historial = new HistorialDeRevisiones(
                entidad.getHistorial().stream().map(MapeadorPreguntaJpa::aEntradaDeHistorial).toList());
        return Pregunta.reconstituir(new PreguntaId(entidad.getId()), new UsuarioId(entidad.getAutorId()), contenido,
                entidad.getEstado(), trazabilidad, historial, entidad.getFechaCreacion(), entidad.getFechaActualizacion());
    }

    /**
     * Copia el estado del agregado sobre su modelo de base de datos (nuevo o ya cargado en la transacción).
     *
     * @param pregunta agregado
     * @param entidad  modelo de base de datos que se va a guardar
     */
    public static void copiarAEntidad(Pregunta pregunta, PreguntaEntidad entidad) {
        ContenidoDePregunta contenido = pregunta.getContenido();
        entidad.setAutorId(pregunta.getAutorId().valor());
        entidad.setContexto(contenido.contexto().texto());
        entidad.setPreguntaDirecta(contenido.preguntaDirecta().texto());
        entidad.setJustificacion(contenido.justificacion().texto());
        entidad.setCompetenciaId(contenido.clasificacion().competenciaId());
        entidad.setTemaId(contenido.clasificacion().temaId());
        entidad.setSubtemaId(contenido.clasificacion().subtemaId());
        entidad.setNivelDificultad(contenido.nivelDificultad());
        entidad.setEstado(pregunta.getEstado());
        entidad.setFechaCreacion(pregunta.getFechaCreacion());
        entidad.setFechaActualizacion(pregunta.getFechaActualizacion());
        reemplazarOpcionesSiCambiaron(contenido.opciones(), entidad);
        reemplazarBibliografiaSiCambio(contenido.bibliografia().referencias(), entidad);
        anexarTrazabilidadNueva(pregunta.getTrazabilidad(), entidad);
        anexarHistorialNuevo(pregunta.getHistorialRevisiones().entradas(), entidad);
    }

    // Las opciones son value objects reemplazables (Taller 1, sección 6): se reescriben solo si cambiaron.
    private static void reemplazarOpcionesSiCambiaron(List<OpcionDeRespuesta> opciones, PreguntaEntidad entidad) {
        List<OpcionDeRespuesta> actuales = entidad.getOpciones().stream()
                .map(opcion -> new OpcionDeRespuesta(opcion.getLetra(), opcion.getTexto(), opcion.isEsCorrecta()))
                .toList();
        if (!actuales.equals(opciones)) {
            entidad.getOpciones().clear();
            opciones.forEach(opcion -> entidad.getOpciones().add(
                    new OpcionEmbebida(opcion.letra(), opcion.texto(), opcion.esCorrecta())));
        }
    }

    private static void reemplazarBibliografiaSiCambio(List<String> referencias, PreguntaEntidad entidad) {
        if (!entidad.getBibliografia().equals(referencias)) {
            entidad.getBibliografia().clear();
            entidad.getBibliografia().addAll(referencias);
        }
    }

    // INV-13: la trazabilidad es de solo anexado; solo se insertan los registros que la entidad aún no tiene.
    private static void anexarTrazabilidadNueva(List<RegistroDeTrazabilidad> registros, PreguntaEntidad entidad) {
        for (int indice = entidad.getTrazabilidad().size(); indice < registros.size(); indice++) {
            RegistroDeTrazabilidad registro = registros.get(indice);
            entidad.getTrazabilidad().add(new RegistroTrazabilidadEmbebido(registro.fecha(), registro.usuarioId().valor(),
                    registro.tipo(), registro.estadoAnterior(), registro.estadoNuevo(), registro.detalle()));
        }
    }

    // INV-14: el historial es de solo anexado; solo se insertan las entradas nuevas.
    private static void anexarHistorialNuevo(List<EntradaDeHistorial> entradas, PreguntaEntidad entidad) {
        for (int indice = entidad.getHistorial().size(); indice < entradas.size(); indice++) {
            entidad.getHistorial().add(aEntidadDeHistorial(entradas.get(indice), indice, entidad));
        }
    }

    private static EntradaHistorialEntidad aEntidadDeHistorial(EntradaDeHistorial entrada, int secuencia, PreguntaEntidad duena) {
        return switch (entrada) {
            case EvaluacionEnHistorial evaluacion -> {
                EntradaHistorialEntidad fila = new EntradaHistorialEntidad(duena, secuencia,
                        EntradaHistorialEntidad.TIPO_EVALUACION, evaluacion.procesoId().valor(), evaluacion.fecha());
                fila.completarEvaluacion(evaluacion.revisorId().valor(), evaluacion.decision(),
                        evaluacion.criterios().stream()
                                .map(criterio -> new CriterioEmbebido(criterio.criterio(), criterio.valoracion()))
                                .toList(),
                        evaluacion.observaciones().stream().map(Observacion::texto).toList());
                yield fila;
            }
            case DictamenEnHistorial dictamen -> {
                EntradaHistorialEntidad fila = new EntradaHistorialEntidad(duena, secuencia,
                        EntradaHistorialEntidad.TIPO_DICTAMEN, dictamen.procesoId().valor(), dictamen.fecha());
                fila.completarDictamen(dictamen.dictamen().resultado(), dictamen.dictamen().porcentajeAprobacion());
                yield fila;
            }
        };
    }

    private static EntradaDeHistorial aEntradaDeHistorial(EntradaHistorialEntidad fila) {
        ProcesoDeRevisionId procesoId = new ProcesoDeRevisionId(fila.getProcesoId());
        if (EntradaHistorialEntidad.TIPO_DICTAMEN.equals(fila.getTipo())) {
            return new DictamenEnHistorial(procesoId,
                    new Dictamen(fila.getResultado(), fila.getPorcentajeAprobacion(), fila.getFecha()));
        }
        return new EvaluacionEnHistorial(procesoId, new UsuarioId(fila.getRevisorId()),
                fila.getCriterios().stream()
                        .map(criterio -> new CriterioEvaluado(criterio.getCriterio(), criterio.getValoracion()))
                        .toList(),
                fila.getObservaciones().stream().map(Observacion::new).toList(),
                fila.getDecision(), fila.getFecha());
    }
}
