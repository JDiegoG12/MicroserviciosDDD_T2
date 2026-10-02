package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.util.ArrayList;
import java.util.List;

/**
 * Colección cronológica y de solo anexado de las evaluaciones y dictámenes de todos los Procesos de
 * revisión de una Pregunta (Taller 1, sección 6; D-15; INV-14).
 *
 * <p>Es un value object: anexar devuelve un historial nuevo y nunca se quita ni se reescribe nada.</p>
 *
 * @param entradas entradas en orden de anexado (lista inmutable)
 */
public record HistorialDeRevisiones(List<EntradaDeHistorial> entradas) {

    /**
     * Valida y copia las entradas.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si la lista es nula
     */
    public HistorialDeRevisiones {
        entradas = List.copyOf(Validaciones.requerirNoNulo(entradas, "historialRevisiones"));
    }

    /**
     * Crea un historial vacío, el de toda Pregunta recién creada.
     *
     * @return historial sin entradas
     */
    public static HistorialDeRevisiones vacio() {
        return new HistorialDeRevisiones(List.of());
    }

    /**
     * Anexa una entrada al final (INV-14: solo anexado).
     *
     * @param entrada evaluación o dictamen a anexar
     * @return un historial nuevo con la entrada al final
     */
    public HistorialDeRevisiones anexar(EntradaDeHistorial entrada) {
        Validaciones.requerirNoNulo(entrada, "entrada");
        List<EntradaDeHistorial> ampliadas = new ArrayList<>(entradas);
        ampliadas.add(entrada);
        return new HistorialDeRevisiones(ampliadas);
    }
}
