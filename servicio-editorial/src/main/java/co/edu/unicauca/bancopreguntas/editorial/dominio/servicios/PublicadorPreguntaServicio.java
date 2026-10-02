package co.edu.unicauca.bancopreguntas.editorial.dominio.servicios;

import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.TransicionNoPermitidaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;

import java.time.Instant;
import java.util.Optional;

/**
 * Servicio de dominio que publica una Pregunta solo si su Proceso de revisión vigente tiene un Dictamen
 * favorable (Taller 1, sección 10.4; CU-08).
 *
 * <p>Verificar el Dictamen exige leer un agregado distinto del que se muta, por eso no vive en la Pregunta.</p>
 */
public final class PublicadorPreguntaServicio {

    /**
     * Verifica el Dictamen del Proceso vigente y ejecuta {@code pregunta.publicar(...)}.
     *
     * @param pregunta        Pregunta {@code APROBADA}
     * @param procesoVigente  Proceso vigente de la Pregunta (abierto o último cerrado), si existe
     * @param administradorId Administrador que publica
     * @param fecha           instante de la publicación
     * @throws TransicionNoPermitidaExcepcion si no hay Proceso con Dictamen {@code APROBADA} o la Pregunta no está {@code APROBADA}
     */
    public void publicar(Pregunta pregunta, Optional<ProcesoDeRevision> procesoVigente, UsuarioId administradorId,
                         Instant fecha) {
        boolean dictamenFavorable = procesoVigente
                .filter(proceso -> proceso.getPreguntaId().equals(pregunta.getId()))
                .map(ProcesoDeRevision::tieneDictamenAprobatorio)
                .orElse(false);
        // CU-08: ninguna Pregunta se publica sin superar la Revisión por pares.
        // CONTRATOS.md 8.1: sin un proceso cerrado con dictamen APROBADA → 409 TRANSICION_NO_PERMITIDA.
        if (!dictamenFavorable) {
            throw new TransicionNoPermitidaExcepcion("La pregunta " + pregunta.getId()
                    + " no tiene un proceso de revisión con dictamen APROBADA y no puede pasar a PUBLICADA.");
        }
        pregunta.publicar(administradorId, fecha);
    }
}
