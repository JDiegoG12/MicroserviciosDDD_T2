package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.AsignacionDeRevisor;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.CriterioEvaluado;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Dictamen;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.FormatoDeEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.FormatoId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Observacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.AsignacionEmbebida;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.CriterioEmbebido;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.FormatoEvaluacionEntidad;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.ProcesoRevisionEntidad;

import java.util.List;

/**
 * Traduce el agregado Proceso de revisión a su modelo de base de datos y viceversa (CONTRATOS.md 3.3.2).
 *
 * <p>Las asignaciones se escriben una sola vez al abrir el Proceso, los formatos solo se anexan (INV-18) y el
 * dictamen, una vez fijado, no se reescribe (INV-21).</p>
 */
public final class MapeadorProcesoJpa {

    private MapeadorProcesoJpa() {
    }

    /**
     * Reconstruye el agregado desde la base de datos.
     *
     * @param entidad modelo de base de datos
     * @return el Proceso reconstituido
     */
    public static ProcesoDeRevision aDominio(ProcesoRevisionEntidad entidad) {
        List<AsignacionDeRevisor> asignaciones = entidad.getAsignaciones().stream()
                .map(asignacion -> new AsignacionDeRevisor(new UsuarioId(asignacion.getRevisorId()), asignacion.getFechaAsignacion()))
                .toList();
        List<FormatoDeEvaluacion> formatos = entidad.getFormatos().stream().map(MapeadorProcesoJpa::aFormato).toList();
        Dictamen dictamen = entidad.getDictamenResultado() == null ? null
                : new Dictamen(entidad.getDictamenResultado(), entidad.getDictamenPorcentaje(), entidad.getDictamenFecha());
        return ProcesoDeRevision.reconstituir(new ProcesoDeRevisionId(entidad.getId()), new PreguntaId(entidad.getPreguntaId()),
                new UsuarioId(entidad.getAutorId()), entidad.getFechaApertura(), asignaciones, formatos, entidad.getEstado(),
                dictamen);
    }

    /**
     * Crea el modelo de base de datos de un Proceso que aún no existe.
     *
     * @param proceso agregado
     * @return entidad nueva con los datos de apertura
     */
    public static ProcesoRevisionEntidad nuevaEntidad(ProcesoDeRevision proceso) {
        return new ProcesoRevisionEntidad(proceso.getId().valor(), proceso.getPreguntaId().valor(),
                proceso.getAutorId().valor(), proceso.getFechaApertura());
    }

    /**
     * Copia el estado del agregado sobre su modelo de base de datos (nuevo o ya cargado en la transacción).
     *
     * @param proceso agregado
     * @param entidad modelo de base de datos que se va a guardar
     */
    public static void copiarAEntidad(ProcesoDeRevision proceso, ProcesoRevisionEntidad entidad) {
        entidad.setEstado(proceso.getEstado());
        if (entidad.getAsignaciones().isEmpty()) {
            proceso.getAsignaciones().forEach(asignacion -> entidad.getAsignaciones().add(
                    new AsignacionEmbebida(asignacion.revisorId().valor(), asignacion.fechaAsignacion())));
        }
        List<FormatoDeEvaluacion> formatos = proceso.getFormatos();
        for (int indice = entidad.getFormatos().size(); indice < formatos.size(); indice++) {
            entidad.getFormatos().add(aEntidadDeFormato(formatos.get(indice), indice, entidad));
        }
        // INV-21: el dictamen emitido es inmutable; solo se escribe la primera vez.
        if (entidad.getDictamenResultado() == null) {
            proceso.getDictamen().ifPresent(dictamen ->
                    entidad.fijarDictamen(dictamen.resultado(), dictamen.porcentajeAprobacion(), dictamen.fechaEmision()));
        }
    }

    private static FormatoEvaluacionEntidad aEntidadDeFormato(FormatoDeEvaluacion formato, int posicion,
                                                              ProcesoRevisionEntidad proceso) {
        return new FormatoEvaluacionEntidad(formato.getId().valor(), proceso, posicion, formato.getRevisorId().valor(),
                formato.getDecision(), formato.getFechaEmision(),
                formato.getCriterios().stream()
                        .map(criterio -> new CriterioEmbebido(criterio.criterio(), criterio.valoracion()))
                        .toList(),
                formato.getObservaciones().stream().map(Observacion::texto).toList());
    }

    private static FormatoDeEvaluacion aFormato(FormatoEvaluacionEntidad fila) {
        return new FormatoDeEvaluacion(new FormatoId(fila.getId()), new UsuarioId(fila.getRevisorId()),
                fila.getCriterios().stream()
                        .map(criterio -> new CriterioEvaluado(criterio.getCriterio(), criterio.getValoracion()))
                        .toList(),
                fila.getObservaciones().stream().map(Observacion::new).toList(),
                fila.getDecision(), fila.getFechaEmision());
    }
}
