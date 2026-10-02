package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.LongitudDeTexto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

/**
 * Motivo obligatorio por el que el Administrador archiva una Pregunta (CU-09; CONTRATOS.md 7.5 y 8.1).
 *
 * @param texto motivo de 1 a 500 caracteres, sin contar solo espacios
 */
public record MotivoDeArchivado(String texto) {

    /** Longitud máxima del motivo (CONTRATOS.md 7.5 y 7.8). */
    public static final int LONGITUD_MAXIMA = 500;

    /**
     * Valida que el motivo exista y tenga de 1 a 500 caracteres.
     *
     * @throws DatoInvalidoExcepcion si el motivo falta, está en blanco o supera 500 caracteres ({@code SOLICITUD_INVALIDA})
     */
    public MotivoDeArchivado {
        Validaciones.requerirNoNulo(texto, "motivo");
        // CONTRATOS.md 4: la longitud se mide en puntos de código, sin los espacios del inicio y del final.
        if (texto.isBlank() || LongitudDeTexto.medir(texto) > LONGITUD_MAXIMA) {
            throw new DatoInvalidoExcepcion("CU-09: el motivo del archivado debe tener entre 1 y "
                    + LONGITUD_MAXIMA + " caracteres.");
        }
    }
}
