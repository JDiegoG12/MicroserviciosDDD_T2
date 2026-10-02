package co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Bibliografia;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ClasificacionAcademica;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ContenidoDePregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Contexto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Justificacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.NivelDeDificultad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.OpcionDeRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaDirecta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.CriterioEvaluado;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Observacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.TipoCriterio;

import java.util.List;

/**
 * Traduce los comandos de aplicación (textos tal como llegan) a los value objects del dominio.
 */
public final class ConversorDeComandos {

    private static final String SIN_TEXTO = "";

    private ConversorDeComandos() {
    }

    /**
     * Convierte los datos de la solicitud en el contenido de la Pregunta.
     *
     * <p>Los textos ausentes se tratan como vacíos para que la Pregunta quede en {@code BORRADOR} (D-02). La
     * clasificación y el nivel de dificultad son obligatorios (CONTRATOS.md 8.1).</p>
     *
     * @param datos datos de la solicitud
     * @return contenido de la Pregunta
     * @throws DatoInvalidoExcepcion si faltan los datos, la clasificación o el nivel, o no tienen el formato del contrato
     */
    public static ContenidoDePregunta aContenido(DatosDePreguntaComando datos) {
        Validaciones.requerirNoNulo(datos, "pregunta");
        return new ContenidoDePregunta(
                new Contexto(textoOVacio(datos.contexto())),
                new PreguntaDirecta(textoOVacio(datos.preguntaDirecta())),
                aOpciones(datos.opciones()),
                new Justificacion(textoOVacio(datos.justificacion())),
                new Bibliografia(datos.bibliografia() == null ? List.of() : datos.bibliografia()),
                ClasificacionAcademica.de(datos.competenciaId(), datos.temaId(), datos.subtemaId()),
                NivelDeDificultad.desdeNombre(datos.nivelDificultad()));
    }

    /**
     * Convierte los criterios de la evaluación.
     *
     * @param criterios criterios de la solicitud
     * @return criterios evaluados del dominio
     * @throws DatoInvalidoExcepcion si faltan, el criterio no existe o la valoración está fuera de rango
     */
    public static List<CriterioEvaluado> aCriterios(List<RegistrarEvaluacionComando.CriterioComando> criterios) {
        Validaciones.requerirNoNulo(criterios, "criterios");
        return criterios.stream()
                .map(criterio -> new CriterioEvaluado(
                        aEnum(TipoCriterio.class, criterio.criterio(), "criterios[].criterio"),
                        Validaciones.requerirNoNulo(criterio.valoracion(), "criterios[].valoracion")))
                .toList();
    }

    /**
     * Convierte las observaciones de la evaluación; una lista ausente equivale a ninguna.
     *
     * @param observaciones textos de la solicitud
     * @return observaciones del dominio
     */
    public static List<Observacion> aObservaciones(List<String> observaciones) {
        return observaciones == null ? List.of() : observaciones.stream().map(Observacion::new).toList();
    }

    /**
     * Convierte el nombre exacto de un enum del contrato (CONTRATOS.md 4: MAYÚSCULAS, sin tildes).
     *
     * @param tipo   clase del enum
     * @param nombre nombre recibido
     * @param campo  nombre del campo, para el mensaje
     * @param <E>    tipo del enum
     * @return el valor del enum
     * @throws DatoInvalidoExcepcion si el nombre es nulo o no existe ({@code SOLICITUD_INVALIDA})
     */
    public static <E extends Enum<E>> E aEnum(Class<E> tipo, String nombre, String campo) {
        Validaciones.requerirNoNulo(nombre, campo);
        try {
            return Enum.valueOf(tipo, nombre);
        } catch (IllegalArgumentException error) {
            throw new DatoInvalidoExcepcion("El valor '" + nombre + "' no es válido para " + campo + ".");
        }
    }

    /**
     * Convierte un enum opcional: nulo si no llega.
     *
     * @param tipo   clase del enum
     * @param nombre nombre recibido o {@code null}
     * @param campo  nombre del campo, para el mensaje
     * @param <E>    tipo del enum
     * @return el valor o {@code null}
     */
    public static <E extends Enum<E>> E aEnumOpcional(Class<E> tipo, String nombre, String campo) {
        return nombre == null ? null : aEnum(tipo, nombre, campo);
    }

    private static List<OpcionDeRespuesta> aOpciones(List<DatosDePreguntaComando.OpcionComando> opciones) {
        if (opciones == null) {
            return List.of();
        }
        return opciones.stream()
                .map(opcion -> new OpcionDeRespuesta(textoOVacio(opcion.letra()), textoOVacio(opcion.texto()),
                        Boolean.TRUE.equals(opcion.esCorrecta())))
                .toList();
    }

    private static String textoOVacio(String texto) {
        return texto == null ? SIN_TEXTO : texto;
    }
}
