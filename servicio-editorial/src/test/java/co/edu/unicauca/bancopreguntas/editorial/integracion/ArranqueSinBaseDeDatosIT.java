package co.edu.unicauca.bancopreguntas.editorial.integracion;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.CatalogoAcademicoPuerto;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.EstadoDelEsquema;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CONTRATOS.md 9.3.6 (v1.10): el servicio arranca con PostgreSQL <strong>detenido</strong>. Mientras no hay base de
 * datos, {@code /salud} responde 200 y la API 503 {@code BASE_DE_DATOS_NO_DISPONIBLE}. Al iniciar el contenedor, el
 * migrador aplica Flyway en segundo plano y la API pasa a responder con normalidad.
 *
 * <p>Usa la configuración de ejecución (sin el perfil {@code pruebas}): {@code ddl-auto=none}, Flyway desactivado al
 * arrancar y sin acceso a metadatos JDBC. El contenedor se publica en un puerto fijo para que la URL del datasource
 * sea conocida antes de que exista la base de datos.</p>
 */
@SpringBootTest(properties = {
        "editorial.migraciones.espera-inicial=500ms",
        "editorial.migraciones.espera-maxima=2s",
        // RabbitMQ no interviene en esta prueba: un puerto cerrado basta (el declarador solo registra avisos).
        "spring.rabbitmq.port=1"})
@AutoConfigureMockMvc
@DisplayName("Arranque sin base de datos y migración en segundo plano (CONTRATOS.md 9.3.6)")
class ArranqueSinBaseDeDatosIT {

    private static final int PUERTO = puertoLibre();

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
            .withCreateContainerCmdModifier(comando -> comando.withHostConfig(comando.getHostConfig()
                    .withPortBindings(new PortBinding(Ports.Binding.bindPort(PUERTO), new ExposedPort(5432)))));

    @MockitoBean
    CatalogoAcademicoPuerto catalogo;

    @Autowired
    MockMvc mvc;

    @Autowired
    EstadoDelEsquema estadoDelEsquema;

    @DynamicPropertySource
    static void baseDeDatosAunDetenida(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:" + PUERTO + "/test");
        registro.add("spring.datasource.username", () -> "test");
        registro.add("spring.datasource.password", () -> "test");
    }

    @AfterAll
    static void detenerBaseDeDatos() {
        POSTGRES.stop();
    }

    private static int puertoLibre() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    @Test
    @DisplayName("Sin base de datos: /salud 200 y API 503; al iniciar PostgreSQL, Flyway migra y la API responde 200")
    void arrancaSinBaseDeDatosYSeRecupera() throws Exception {
        assertThat(estadoDelEsquema.esquemaListo()).isFalse();
        mvc.perform(get("/salud")).andExpect(status().isOk());
        mvc.perform(get("/openapi.json")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/preguntas").header("X-Usuario-Id", "11111111-1111-4111-8111-000000000001")
                        .header("X-Roles", "ADMINISTRADOR"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("BASE_DE_DATOS_NO_DISPONIBLE"));

        POSTGRES.start();
        long limite = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (!estadoDelEsquema.esquemaListo() && System.nanoTime() < limite) {
            Thread.sleep(200);
        }

        assertThat(estadoDelEsquema.esquemaListo()).as("el migrador debe aplicar Flyway al iniciar PostgreSQL").isTrue();
        mvc.perform(get("/api/v1/preguntas").header("X-Usuario-Id", "11111111-1111-4111-8111-000000000001")
                        .header("X-Roles", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }
}
