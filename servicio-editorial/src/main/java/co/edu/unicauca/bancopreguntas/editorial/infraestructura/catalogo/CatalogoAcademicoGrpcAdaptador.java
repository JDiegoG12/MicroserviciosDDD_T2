package co.edu.unicauca.bancopreguntas.editorial.infraestructura.catalogo;

import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.CatalogoAcademicoGrpc;
import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.MotivoRechazo;
import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.ValidarClasificacionRespuesta;
import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.ValidarClasificacionSolicitud;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.CatalogoNoDisponibleExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.CatalogoAcademicoPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.MotivoRechazoClasificacion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.ResultadoValidacionClasificacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ClasificacionAcademica;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.correlacion.ContextoCorrelacion;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Cliente gRPC de {@code CatalogoAcademico.ValidarClasificacion} (CONTRATOS.md 6, comunicación C2). Implementa
 * {@link CatalogoAcademicoPuerto}.
 *
 * <p>Traducción exacta de la tabla de comportamiento de la sección 6:</p>
 * <ul>
 *   <li>{@code valida=true} → resultado válido; {@code valida=false} → resultado inválido con su motivo y
 *       detalle, que el caso de uso convierte en 422 {@code CLASIFICACION_INVALIDA};</li>
 *   <li>estado {@code INVALID_ARGUMENT} (algún id no es UUID) → 400 {@code SOLICITUD_INVALIDA};</li>
 *   <li>{@code UNAVAILABLE}, {@code DEADLINE_EXCEEDED} (más de 2 s) y cualquier otro error → 503
 *       {@code CATALOGO_NO_DISPONIBLE}.</li>
 * </ul>
 * <p>Cada llamada lleva el metadato {@code x-id-correlacion} (CONTRATOS.md 4.1). El canal es perezoso: se conecta
 * en la primera llamada, así que el servicio arranca aunque Catálogo esté caído (9.3.6).</p>
 */
public class CatalogoAcademicoGrpcAdaptador implements CatalogoAcademicoPuerto {

    /** Nombre exacto del metadato de correlación (CONTRATOS.md 4.1 y 6). */
    public static final Metadata.Key<String> METADATO_CORRELACION =
            Metadata.Key.of("x-id-correlacion", Metadata.ASCII_STRING_MARSHALLER);

    private static final Logger LOG = LoggerFactory.getLogger(CatalogoAcademicoGrpcAdaptador.class);
    private static final String PREFIJO_MOTIVO = "MOTIVO_RECHAZO_";

    private final CatalogoAcademicoGrpc.CatalogoAcademicoBlockingStub stub;
    private final Duration plazoMaximo;

    /**
     * Crea el adaptador.
     *
     * @param canal       canal gRPC hacia Catálogo (texto plano, perezoso)
     * @param plazoMaximo deadline por llamada (2 s según CONTRATOS.md 6)
     */
    public CatalogoAcademicoGrpcAdaptador(Channel canal, Duration plazoMaximo) {
        this.stub = CatalogoAcademicoGrpc.newBlockingStub(canal);
        this.plazoMaximo = plazoMaximo;
    }

    /**
     * {@inheritDoc}
     *
     * @throws DatoInvalidoExcepcion          si Catálogo responde {@code INVALID_ARGUMENT}
     * @throws CatalogoNoDisponibleExcepcion si Catálogo no responde a tiempo o falla
     */
    @Override
    public ResultadoValidacionClasificacion validarClasificacion(ClasificacionAcademica clasificacion) {
        ValidarClasificacionSolicitud solicitud = ValidarClasificacionSolicitud.newBuilder()
                .setCompetenciaId(clasificacion.competenciaId().toString())
                .setTemaId(clasificacion.temaId().toString())
                .setSubtemaId(clasificacion.subtemaId().toString())
                .build();
        try {
            ValidarClasificacionRespuesta respuesta = stubConCorrelacion()
                    .withDeadlineAfter(plazoMaximo.toMillis(), TimeUnit.MILLISECONDS)
                    .validarClasificacion(solicitud);
            return aResultado(respuesta);
        } catch (StatusRuntimeException error) {
            throw traducirError(error);
        }
    }

    private CatalogoAcademicoGrpc.CatalogoAcademicoBlockingStub stubConCorrelacion() {
        return ContextoCorrelacion.actual()
                .map(idCorrelacion -> {
                    Metadata metadatos = new Metadata();
                    metadatos.put(METADATO_CORRELACION, idCorrelacion);
                    return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadatos));
                })
                .orElse(stub);
    }

    private static ResultadoValidacionClasificacion aResultado(ValidarClasificacionRespuesta respuesta) {
        if (respuesta.getValida()) {
            return ResultadoValidacionClasificacion.valido();
        }
        return ResultadoValidacionClasificacion.invalido(aMotivo(respuesta.getMotivo()), respuesta.getDetalle());
    }

    // El enum del .proto lleva el prefijo MOTIVO_RECHAZO_; el del puerto, no.
    private static MotivoRechazoClasificacion aMotivo(MotivoRechazo motivo) {
        if (motivo == MotivoRechazo.UNRECOGNIZED) {
            return MotivoRechazoClasificacion.NINGUNO;
        }
        return MotivoRechazoClasificacion.valueOf(motivo.name().substring(PREFIJO_MOTIVO.length()));
    }

    private static RuntimeException traducirError(StatusRuntimeException error) {
        Status.Code codigo = error.getStatus().getCode();
        if (codigo == Status.Code.INVALID_ARGUMENT) {
            return new DatoInvalidoExcepcion("Catálogo rechazó la clasificación por formato: "
                    + error.getStatus().getDescription());
        }
        LOG.warn("Catálogo no disponible (estado gRPC {}): {}", codigo, error.getStatus().getDescription());
        return new CatalogoNoDisponibleExcepcion("El Catálogo Académico no respondió (estado gRPC " + codigo + ").", error);
    }
}
