package co.edu.unicauca.bancopreguntas.editorial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque del microservicio {@code servicio-editorial} (Gestión Editorial de Preguntas).
 *
 * <p>En la etapa 1 solo arranca el contexto de Spring. El cableado de los casos de uso, los adaptadores
 * REST, JPA, gRPC y RabbitMQ se agregan en la etapa 2 dentro de {@code infraestructura} e {@code interfaces}.</p>
 */
@SpringBootApplication
public class ServicioEditorialAplicacion {

    /**
     * Arranca la aplicación.
     *
     * @param argumentos argumentos de línea de comandos
     */
    public static void main(String[] argumentos) {
        SpringApplication.run(ServicioEditorialAplicacion.class, argumentos);
    }
}
