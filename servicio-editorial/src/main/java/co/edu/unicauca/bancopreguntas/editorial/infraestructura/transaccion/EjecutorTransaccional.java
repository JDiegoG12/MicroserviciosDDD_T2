package co.edu.unicauca.bancopreguntas.editorial.infraestructura.transaccion;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Decorador transaccional de los casos de uso. La capa de aplicación no conoce Spring: la transacción la abre la
 * infraestructura alrededor de cada caso de uso completo (Etapa 2, "Capas y Spring").
 *
 * <p>Así, en los casos de uso que guardan dos agregados (excepción a 3.3.5, CONTRATOS.md 11.1) ambos guardados
 * quedan en la misma transacción local: si uno falla, ninguno se confirma. Además, los eventos de integración se
 * publican después del commit (CONTRATOS.md 7.7).</p>
 */
public class EjecutorTransaccional {

    private final TransactionTemplate escritura;
    private final TransactionTemplate lectura;

    /**
     * Crea el ejecutor sobre el gestor de transacciones de JPA.
     *
     * @param gestorDeTransacciones gestor de transacciones
     */
    public EjecutorTransaccional(PlatformTransactionManager gestorDeTransacciones) {
        this.escritura = new TransactionTemplate(gestorDeTransacciones);
        this.lectura = new TransactionTemplate(gestorDeTransacciones);
        this.lectura.setReadOnly(true);
    }

    /**
     * Ejecuta un caso de uso que modifica datos dentro de una transacción. Si lanza una excepción, la transacción
     * se revierte y la excepción se propaga sin cambios.
     *
     * @param casoDeUso invocación del caso de uso
     * @param <T>       tipo del resultado
     * @return el resultado del caso de uso
     */
    public <T> T escribir(Supplier<T> casoDeUso) {
        return escritura.execute(estado -> casoDeUso.get());
    }

    /**
     * Ejecuta un caso de uso de solo consulta dentro de una transacción de solo lectura.
     *
     * @param casoDeUso invocación del caso de uso
     * @param <T>       tipo del resultado
     * @return el resultado del caso de uso
     */
    public <T> T leer(Supplier<T> casoDeUso) {
        return lectura.execute(estado -> casoDeUso.get());
    }
}
