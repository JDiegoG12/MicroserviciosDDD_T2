package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta.*;

/**
 * Tabla de transiciones permitidas del ciclo de vida: el diagrama de CONTRATOS.md 11.1, flecha por flecha
 * (INV-09, RF-15).
 *
 * <p>Cualquier par (origen, destino) que no aparezca aquí es una transición no permitida.</p>
 */
public final class TransicionesDePregunta {

    private static final Map<EstadoPregunta, Set<EstadoPregunta>> PERMITIDAS = new EnumMap<>(EstadoPregunta.class);

    static {
        // D-02: supera la validación estructural.
        PERMITIDAS.put(BORRADOR, Set.of(EN_CONSTRUCCION));
        // D-02: una modificación la deja incompleta / CU-07: envío a revisión.
        PERMITIDAS.put(EN_CONSTRUCCION, Set.of(BORRADOR, PENDIENTE_REVISION));
        // CU-10, D-14: asignar revisores.
        PERMITIDAS.put(PENDIENTE_REVISION, Set.of(EN_REVISION));
        // CU-12, INV-20: el dictamen decide.
        PERMITIDAS.put(EN_REVISION, Set.of(APROBADA, RECHAZADA));
        // D-07: automático.
        PERMITIDAS.put(RECHAZADA, Set.of(EN_CONSTRUCCION));
        // CU-08: publicación.
        PERMITIDAS.put(APROBADA, Set.of(PUBLICADA));
        // CU-09: archivado.
        PERMITIDAS.put(PUBLICADA, Set.of(ARCHIVADA));
        // Estado final: sin salidas.
        PERMITIDAS.put(ARCHIVADA, Set.of());
    }

    private TransicionesDePregunta() {
    }

    /**
     * Consulta si la máquina de estados declara la transición.
     *
     * @param origen  estado actual
     * @param destino estado al que se quiere pasar
     * @return {@code true} si la transición está en la tabla
     */
    public static boolean estaPermitida(EstadoPregunta origen, EstadoPregunta destino) {
        return PERMITIDAS.getOrDefault(origen, Set.of()).contains(destino);
    }

    /**
     * Devuelve los destinos permitidos desde un estado.
     *
     * @param origen estado actual
     * @return conjunto inmutable de destinos
     */
    public static Set<EstadoPregunta> destinosDesde(EstadoPregunta origen) {
        return PERMITIDAS.getOrDefault(origen, Set.of());
    }
}
