package co.edu.unicauca.bancopreguntas.editorial.infraestructura.catalogo;

import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.CatalogoAcademicoGrpc;
import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.MotivoRechazo;
import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.ValidarClasificacionRespuesta;
import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.ValidarClasificacionSolicitud;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.MotivoRechazoClasificacion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.ResultadoValidacionClasificacion;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.correlacion.ContextoCorrelacion;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.CLASIFICACION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del cliente gRPC contra un servidor <strong>falso en proceso</strong> generado del mismo {@code .proto}
 * (CONTRATOS.md 6): respuesta válida, cada motivo de rechazo, id mal formado, servidor caído y servidor que no
 * responde (deadline).
 */
@DisplayName("CatalogoAcademicoGrpcAdaptador (CONTRATOS.md 6)")
class CatalogoAcademicoGrpcAdaptadorTest {

    /** Plazo de las llamadas de prueba (el de producción sigue en 2 s, CONTRATOS.md 6). */
    private static final Duration PLAZO_DE_PRUEBA = Duration.ofSeconds(1);
    /** Plazo amplio de la llamada de calentamiento. */
    private static final Duration PLAZO_DE_CALENTAMIENTO = Duration.ofSeconds(10);

    private final AtomicReference<BiConsumer<ValidarClasificacionSolicitud, StreamObserver<ValidarClasificacionRespuesta>>> comportamiento =
            new AtomicReference<>();
    private final AtomicReference<String> correlacionRecibida = new AtomicReference<>();
    private final AtomicReference<ValidarClasificacionSolicitud> solicitudRecibida = new AtomicReference<>();
    private Server servidor;
    private ManagedChannel canal;
    private CatalogoAcademicoGrpcAdaptador adaptador;

    @BeforeEach
    void levantarServidorFalso() throws IOException {
        String nombre = InProcessServerBuilder.generateName();
        CatalogoAcademicoGrpc.CatalogoAcademicoImplBase servicio = new CatalogoAcademicoGrpc.CatalogoAcademicoImplBase() {
            @Override
            public void validarClasificacion(ValidarClasificacionSolicitud solicitud,
                                             StreamObserver<ValidarClasificacionRespuesta> respuesta) {
                solicitudRecibida.set(solicitud);
                comportamiento.get().accept(solicitud, respuesta);
            }
        };
        ServerInterceptor capturaDeMetadatos = new ServerInterceptor() {
            @Override
            public <S, R> ServerCall.Listener<S> interceptCall(ServerCall<S, R> llamada, Metadata metadatos,
                                                                ServerCallHandler<S, R> siguiente) {
                correlacionRecibida.set(metadatos.get(CatalogoAcademicoGrpcAdaptador.METADATO_CORRELACION));
                return siguiente.startCall(llamada, metadatos);
            }
        };
        servidor = InProcessServerBuilder.forName(nombre).directExecutor()
                .addService(ServerInterceptors.intercept(servicio, capturaDeMetadatos)).build().start();
        canal = InProcessChannelBuilder.forName(nombre).directExecutor().build();
        adaptador = new CatalogoAcademicoGrpcAdaptador(canal, PLAZO_DE_PRUEBA);
        calentarCanal();
    }

    // En frío, la primera llamada carga clases de gRPC y protobuf y podía agotar el plazo de prueba (caso
    // ternaInvalida[1]). Se hace una llamada previa con un plazo amplio y luego se limpia lo capturado.
    private void calentarCanal() {
        responder(ValidarClasificacionRespuesta.newBuilder().setValida(true).build());
        new CatalogoAcademicoGrpcAdaptador(canal, PLAZO_DE_CALENTAMIENTO).validarClasificacion(CLASIFICACION);
        solicitudRecibida.set(null);
        correlacionRecibida.set(null);
    }

    @AfterEach
    void detenerServidor() {
        ContextoCorrelacion.limpiar();
        canal.shutdownNow();
        servidor.shutdownNow();
    }

    private void responder(ValidarClasificacionRespuesta respuesta) {
        comportamiento.set((solicitud, observador) -> {
            observador.onNext(respuesta);
            observador.onCompleted();
        });
    }

    @Test
    @DisplayName("Terna válida → resultado válido; la solicitud lleva los tres UUID en texto")
    void ternaValida() {
        responder(ValidarClasificacionRespuesta.newBuilder().setValida(true)
                .setMotivo(MotivoRechazo.MOTIVO_RECHAZO_NINGUNO).build());

        ResultadoValidacionClasificacion resultado = adaptador.validarClasificacion(CLASIFICACION);

        assertThat(resultado.valida()).isTrue();
        assertThat(solicitudRecibida.get().getCompetenciaId()).isEqualTo("22222222-2222-4222-8222-000000000101");
        assertThat(solicitudRecibida.get().getTemaId()).isEqualTo("22222222-2222-4222-8222-000000000201");
        assertThat(solicitudRecibida.get().getSubtemaId()).isEqualTo("22222222-2222-4222-8222-000000000301");
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "MOTIVO_RECHAZO_COMPETENCIA_INEXISTENTE, COMPETENCIA_INEXISTENTE",
            "MOTIVO_RECHAZO_TEMA_INEXISTENTE, TEMA_INEXISTENTE",
            "MOTIVO_RECHAZO_TEMA_NO_PERTENECE_A_COMPETENCIA, TEMA_NO_PERTENECE_A_COMPETENCIA",
            "MOTIVO_RECHAZO_SUBTEMA_INEXISTENTE, SUBTEMA_INEXISTENTE",
            "MOTIVO_RECHAZO_SUBTEMA_NO_PERTENECE_A_TEMA, SUBTEMA_NO_PERTENECE_A_TEMA"})
    @DisplayName("Terna inválida → resultado inválido con su motivo y el detalle de Catálogo")
    void ternaInvalida(MotivoRechazo motivoGrpc, MotivoRechazoClasificacion motivoEsperado) {
        responder(ValidarClasificacionRespuesta.newBuilder().setValida(false).setMotivo(motivoGrpc)
                .setDetalle("Detalle legible").build());

        ResultadoValidacionClasificacion resultado = adaptador.validarClasificacion(CLASIFICACION);

        assertThat(resultado.valida()).isFalse();
        assertThat(resultado.motivo()).isEqualTo(motivoEsperado);
        assertThat(resultado.detalle()).isEqualTo("Detalle legible");
    }

    @Test
    @DisplayName("INVALID_ARGUMENT (id mal formado) → SOLICITUD_INVALIDA")
    void idMalFormado() {
        comportamiento.set((solicitud, observador) ->
                observador.onError(Status.INVALID_ARGUMENT.withDescription("temaId no es UUID").asRuntimeException()));

        lanzaConCodigo(() -> adaptador.validarClasificacion(CLASIFICACION), "SOLICITUD_INVALIDA");
    }

    @Test
    @DisplayName("UNAVAILABLE o cualquier otro error → CATALOGO_NO_DISPONIBLE")
    void servidorCaido() {
        comportamiento.set((solicitud, observador) -> observador.onError(Status.UNAVAILABLE.asRuntimeException()));
        lanzaConCodigo(() -> adaptador.validarClasificacion(CLASIFICACION), "CATALOGO_NO_DISPONIBLE");

        comportamiento.set((solicitud, observador) -> observador.onError(Status.INTERNAL.asRuntimeException()));
        lanzaConCodigo(() -> adaptador.validarClasificacion(CLASIFICACION), "CATALOGO_NO_DISPONIBLE");
    }

    @Test
    @DisplayName("Servidor que no responde dentro del plazo (DEADLINE_EXCEEDED) → CATALOGO_NO_DISPONIBLE")
    void servidorQueNoResponde() {
        comportamiento.set((solicitud, observador) -> {
            // Nunca responde: el cliente debe cortar por deadline.
        });
        long inicio = System.nanoTime();

        lanzaConCodigo(() -> adaptador.validarClasificacion(CLASIFICACION), "CATALOGO_NO_DISPONIBLE");

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("Propaga el metadato x-id-correlacion de la petición en curso")
    void propagaCorrelacion() {
        responder(ValidarClasificacionRespuesta.newBuilder().setValida(true).build());
        ContextoCorrelacion.establecer("0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b");

        adaptador.validarClasificacion(CLASIFICACION);

        assertThat(correlacionRecibida.get()).isEqualTo("0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b");
    }
}
