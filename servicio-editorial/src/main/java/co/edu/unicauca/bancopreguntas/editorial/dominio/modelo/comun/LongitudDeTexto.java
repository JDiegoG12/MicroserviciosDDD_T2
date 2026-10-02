package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun;

/**
 * Regla común de medición de textos (CONTRATOS.md 4, "Longitud de textos"): todos los límites de longitud se
 * cuentan en caracteres Unicode (puntos de código), después de quitar los espacios del inicio y del final.
 */
public final class LongitudDeTexto {

    private LongitudDeTexto() {
    }

    /**
     * Mide un texto según la regla común.
     *
     * @param texto texto a medir; no nulo
     * @return cantidad de puntos de código tras quitar los espacios del inicio y del final
     */
    public static int medir(String texto) {
        String recortado = texto.strip();
        return recortado.codePointCount(0, recortado.length());
    }
}
