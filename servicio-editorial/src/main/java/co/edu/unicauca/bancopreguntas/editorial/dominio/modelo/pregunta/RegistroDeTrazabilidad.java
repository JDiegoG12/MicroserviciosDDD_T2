package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.time.Instant;

/**
 * Anotación inmutable de un cambio de contenido o de estado de la Pregunta, con fecha y usuario
 * responsable (RF-29, RF-30, INV-13).
 *
 * @param fecha          instante UTC del cambio
 * @param usuarioId      usuario responsable ({@code X-Usuario-Id})
 * @param tipo           {@code CREACION}, {@code MODIFICACION} o {@code TRANSICION}
 * @param estadoAnterior estado antes del cambio; {@code null} en la creación
 * @param estadoNuevo    estado después del cambio
 * @param detalle        descripción legible del cambio
 */
public record RegistroDeTrazabilidad(
        Instant fecha,
        UsuarioId usuarioId,
        TipoDeRegistro tipo,
        EstadoPregunta estadoAnterior,
        EstadoPregunta estadoNuevo,
        String detalle) {

    /**
     * Exige los datos obligatorios del registro (RF-30: fecha y usuario responsable).
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si falta un dato obligatorio
     */
    public RegistroDeTrazabilidad {
        Validaciones.requerirNoNulo(fecha, "fecha");
        Validaciones.requerirNoNulo(usuarioId, "usuarioId");
        Validaciones.requerirNoNulo(tipo, "tipo");
        Validaciones.requerirNoNulo(estadoNuevo, "estadoNuevo");
        Validaciones.requerirNoNulo(detalle, "detalle");
    }
}
