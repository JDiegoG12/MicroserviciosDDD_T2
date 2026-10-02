package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.seguridad;

import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.EstadoDelEsquema;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registra en Spring MVC el resolutor de {@code UsuarioActual} y el interceptor que responde 503 mientras el esquema
 * no está listo (CONTRATOS.md 9.3.6).
 */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final ResolutorUsuarioActual resolutorUsuarioActual;
    private final EstadoDelEsquema estadoDelEsquema;

    /**
     * Crea la configuración.
     *
     * @param resolutorUsuarioActual resolutor de identidad desde los encabezados
     * @param estadoDelEsquema       estado de las migraciones
     */
    public ConfiguracionWeb(ResolutorUsuarioActual resolutorUsuarioActual, EstadoDelEsquema estadoDelEsquema) {
        this.resolutorUsuarioActual = resolutorUsuarioActual;
        this.estadoDelEsquema = estadoDelEsquema;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolutores) {
        resolutores.add(resolutorUsuarioActual);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new InterceptorEsquemaListo(estadoDelEsquema)).addPathPatterns("/api/v1/**");
    }
}
