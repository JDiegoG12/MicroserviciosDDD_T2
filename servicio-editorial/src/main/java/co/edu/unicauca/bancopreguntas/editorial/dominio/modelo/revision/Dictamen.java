package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Resultado consolidado de la Revisión por pares; inmutable una vez emitido (INV-20, INV-21).
 *
 * @param resultado            {@code APROBADA} o {@code RECHAZADA}
 * @param porcentajeAprobacion aprobatorias / asignados × 100, con 2 decimales (CONTRATOS.md 4 y 8.1)
 * @param fechaEmision         instante UTC en que se emitió
 */
public record Dictamen(ResultadoDictamen resultado, BigDecimal porcentajeAprobacion, Instant fechaEmision) {

    /** Umbral de aprobación en porcentaje; se exige superarlo estrictamente (INV-20, D-04). */
    public static final int UMBRAL_APROBACION_PORCENTAJE = 70;

    /** Decimales con que se expresa el porcentaje (CONTRATOS.md 4: puntajes con 2 decimales). */
    public static final int DECIMALES_PORCENTAJE = 2;

    /**
     * Exige los tres datos del dictamen.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si falta alguno
     */
    public Dictamen {
        Validaciones.requerirNoNulo(resultado, "resultado");
        Validaciones.requerirNoNulo(porcentajeAprobacion, "porcentajeAprobacion");
        Validaciones.requerirNoNulo(fechaEmision, "fechaEmision");
    }

    /**
     * Calcula el dictamen a partir de las evaluaciones aprobatorias y del total de Revisores asignados.
     *
     * <p>INV-20: el dictamen aprueba solo si el porcentaje es estrictamente mayor a 70 %. La comparación
     * se hace con enteros ({@code aprobatorias × 100 > 70 × asignados}) para que el redondeo a 2
     * decimales no cambie el resultado. Con 2 o 3 revisores equivale a unanimidad (D-05).</p>
     *
     * @param aprobatorias cantidad de evaluaciones {@code APROBATORIA}
     * @param asignados    cantidad de Revisores asignados (mayor que cero)
     * @param fechaEmision instante de emisión
     * @return el dictamen calculado
     */
    public static Dictamen calcular(int aprobatorias, int asignados, Instant fechaEmision) {
        BigDecimal porcentaje = BigDecimal.valueOf(aprobatorias * 100L)
                .divide(BigDecimal.valueOf(asignados), DECIMALES_PORCENTAJE, RoundingMode.HALF_UP);
        boolean superaElUmbral = (long) aprobatorias * 100 > (long) UMBRAL_APROBACION_PORCENTAJE * asignados;
        ResultadoDictamen resultado = superaElUmbral ? ResultadoDictamen.APROBADA : ResultadoDictamen.RECHAZADA;
        return new Dictamen(resultado, porcentaje, fechaEmision);
    }

    /**
     * Indica si el dictamen es favorable.
     *
     * @return {@code true} si el resultado es {@code APROBADA}
     */
    public boolean esAprobatorio() {
        return resultado == ResultadoDictamen.APROBADA;
    }
}
