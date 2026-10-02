package co.edu.unicauca.bancopreguntas.editorial.infraestructura.catalogo;

import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.CatalogoAcademicoGrpc;
import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.MotivoRechazo;
import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.ValidarClasificacionRespuesta;
import co.edu.unicauca.bancopreguntas.contratos.catalogo.v1.ValidarClasificacionSolicitud;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

/**
 * Servidor gRPC <strong>falso</strong> de {@code CatalogoAcademico} (mismo {@code .proto} de CONTRATOS.md 6) para las
 * pruebas de humo de Editorial mientras {@code servicio-catalogo} no está disponible. Acepta cualquier clasificación.
 *
 * <p>Uso: {@code java -cp <clases de prueba y dependencias> ...ServidorCatalogoFalso [puerto]} (por defecto 50051). No
 * forma parte de la imagen: vive en las fuentes de prueba.</p>
 */
public final class ServidorCatalogoFalso {

    private ServidorCatalogoFalso() {
    }

    /**
     * Arranca el servidor y espera hasta que se detenga el proceso.
     *
     * @param argumentos puerto opcional
     * @throws IOException          si no se puede abrir el puerto
     * @throws InterruptedException si se interrumpe la espera
     */
    public static void main(String[] argumentos) throws IOException, InterruptedException {
        int puerto = argumentos.length > 0 ? Integer.parseInt(argumentos[0]) : 50051;
        Server servidor = ServerBuilder.forPort(puerto).addService(new CatalogoAcademicoGrpc.CatalogoAcademicoImplBase() {
            @Override
            public void validarClasificacion(ValidarClasificacionSolicitud solicitud,
                                             StreamObserver<ValidarClasificacionRespuesta> respuesta) {
                System.out.println("ValidarClasificacion: " + solicitud.getCompetenciaId() + " / " + solicitud.getTemaId()
                        + " / " + solicitud.getSubtemaId());
                respuesta.onNext(ValidarClasificacionRespuesta.newBuilder().setValida(true)
                        .setMotivo(MotivoRechazo.MOTIVO_RECHAZO_NINGUNO).build());
                respuesta.onCompleted();
            }
        }).build().start();
        System.out.println("Servidor de Catálogo falso escuchando en el puerto " + puerto);
        servidor.awaitTermination();
    }
}
