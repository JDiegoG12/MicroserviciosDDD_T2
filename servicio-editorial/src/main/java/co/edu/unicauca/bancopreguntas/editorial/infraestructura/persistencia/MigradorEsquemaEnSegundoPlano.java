package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * Aplica las migraciones de Flyway <strong>en segundo plano</strong>, con reintentos y espera progresiva, para que el
 * servicio arranque aunque PostgreSQL no esté listo (CONTRATOS.md 9.3.6, v1.10).
 *
 * <p>La migración automática de Spring está desactivada ({@code spring.flyway.enabled=false}). Este componente se
 * lanza cuando la aplicación está lista, reintenta hasta lograrlo (la espera se duplica en cada intento, con un tope de
 * 30 s), escribe cada intento en el log y expone {@link #esquemaListo()} para que la API responda 503 mientras tanto.
 * La validación del esquema frente a las entidades se hace en las pruebas de integración, no al arrancar.</p>
 */
public class MigradorEsquemaEnSegundoPlano implements EstadoDelEsquema {

    private static final Logger LOG = LoggerFactory.getLogger(MigradorEsquemaEnSegundoPlano.class);
    private static final String UBICACION_MIGRACIONES = "classpath:db/migration";

    private final DataSource fuenteDeDatos;
    private final Duration esperaInicial;
    private final Duration esperaMaxima;
    private volatile boolean listo;

    /**
     * Crea el migrador.
     *
     * @param fuenteDeDatos conexión a PostgreSQL (Hikari, sin fallar al iniciar)
     * @param esperaInicial espera antes del segundo intento
     * @param esperaMaxima  tope de espera entre intentos (30 s según 9.3.6)
     */
    public MigradorEsquemaEnSegundoPlano(DataSource fuenteDeDatos, Duration esperaInicial, Duration esperaMaxima) {
        this.fuenteDeDatos = fuenteDeDatos;
        this.esperaInicial = esperaInicial;
        this.esperaMaxima = esperaMaxima;
    }

    @Override
    public boolean esquemaListo() {
        return listo;
    }

    /**
     * Lanza la migración en un hilo aparte cuando la aplicación está lista; el arranque no espera a la base de datos.
     *
     * @param evento aplicación lista
     */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar(ApplicationReadyEvent evento) {
        Thread.ofVirtual().name("migrador-esquema").start(this::migrarConReintentos);
    }

    /**
     * Ejecuta {@code Flyway.migrate()} hasta lograrlo, con espera progresiva entre intentos.
     */
    void migrarConReintentos() {
        Flyway flyway = Flyway.configure().dataSource(fuenteDeDatos).locations(UBICACION_MIGRACIONES).load();
        Duration espera = esperaInicial;
        int intento = 1;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                LOG.info("Migraciones de Flyway: intento {}.", intento);
                MigrateResult resultado = flyway.migrate();
                listo = true;
                LOG.info("Migraciones de Flyway aplicadas: {} nuevas; esquema en la versión {}. Esquema listo.",
                        resultado.migrationsExecuted, resultado.targetSchemaVersion == null ? "actual" : resultado.targetSchemaVersion);
                return;
            } catch (RuntimeException error) {
                LOG.warn("Migraciones de Flyway: intento {} fallido; nuevo intento en {} s: {}", intento,
                        espera.toSeconds(), error.getMessage());
                dormir(espera);
                espera = espera.multipliedBy(2).compareTo(esperaMaxima) > 0 ? esperaMaxima : espera.multipliedBy(2);
                intento++;
            }
        }
    }

    private static void dormir(Duration espera) {
        try {
            Thread.sleep(espera);
        } catch (InterruptedException interrupcion) {
            Thread.currentThread().interrupt();
        }
    }
}
