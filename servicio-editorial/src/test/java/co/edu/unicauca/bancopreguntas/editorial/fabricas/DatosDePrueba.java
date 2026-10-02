package co.edu.unicauca.bancopreguntas.editorial.fabricas;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Bibliografia;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ClasificacionAcademica;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ContenidoDePregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Contexto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.HistorialDeRevisiones;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Justificacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.NivelDeDificultad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.OpcionDeRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaDirecta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.CriterioEvaluado;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Observacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.TipoCriterio;

import java.time.Instant;
import java.util.List;

/**
 * Datos de prueba compartidos: usuarios de CONTRATOS.md 4.2, catálogo semilla de 4.3 y una Pregunta
 * válida de ejemplo (la de CONTRATOS.md 7.4).
 */
public final class DatosDePrueba {

    /** Administrador (CONTRATOS.md 4.2). */
    public static final UsuarioId ADMINISTRADOR = UsuarioId.de("11111111-1111-4111-8111-000000000001");
    /** Autor (CONTRATOS.md 4.2). */
    public static final UsuarioId AUTOR = UsuarioId.de("11111111-1111-4111-8111-000000000002");
    /** Revisor 1 (CONTRATOS.md 4.2). */
    public static final UsuarioId REVISOR_1 = UsuarioId.de("11111111-1111-4111-8111-000000000003");
    /** Revisor 2 (CONTRATOS.md 4.2). */
    public static final UsuarioId REVISOR_2 = UsuarioId.de("11111111-1111-4111-8111-000000000004");
    /** Revisor 3 (CONTRATOS.md 4.2). */
    public static final UsuarioId REVISOR_3 = UsuarioId.de("11111111-1111-4111-8111-000000000007");
    /** Docente (CONTRATOS.md 4.2). */
    public static final UsuarioId DOCENTE = UsuarioId.de("11111111-1111-4111-8111-000000000005");
    /** Estudiante (CONTRATOS.md 4.2). */
    public static final UsuarioId ESTUDIANTE = UsuarioId.de("11111111-1111-4111-8111-000000000006");
    /** Un cuarto revisor, solo para los casos de 4 revisores del dictamen (se usa el id del Docente). */
    public static final UsuarioId REVISOR_4 = DOCENTE;

    /** Razonamiento cuantitativo / Estadística / Medidas de tendencia central (CONTRATOS.md 4.3). */
    public static final ClasificacionAcademica CLASIFICACION = ClasificacionAcademica.de(
            "22222222-2222-4222-8222-000000000101",
            "22222222-2222-4222-8222-000000000201",
            "22222222-2222-4222-8222-000000000301");

    /** Diseño de software / Arquitectura de software / Microservicios (CONTRATOS.md 4.3). */
    public static final ClasificacionAcademica OTRA_CLASIFICACION = ClasificacionAcademica.de(
            "22222222-2222-4222-8222-000000000102",
            "22222222-2222-4222-8222-000000000204",
            "22222222-2222-4222-8222-000000000305");

    /** Instante base de las pruebas (CONTRATOS.md 4: ISO-8601 UTC). */
    public static final Instant FECHA = Instant.parse("2026-10-01T15:30:00Z");

    private DatosDePrueba() {
    }

    /**
     * Instante a una cantidad de minutos de {@link #FECHA}.
     *
     * @param minutos minutos a sumar
     * @return el instante
     */
    public static Instant fecha(int minutos) {
        return FECHA.plusSeconds(minutos * 60L);
    }

    /**
     * Opciones válidas de la pregunta de ejemplo; la correcta es la C.
     *
     * @return las cuatro opciones
     */
    public static List<OpcionDeRespuesta> opcionesValidas() {
        return List.of(
                new OpcionDeRespuesta("A", "3,0", false),
                new OpcionDeRespuesta("B", "3,8", false),
                new OpcionDeRespuesta("C", "4,0", true),
                new OpcionDeRespuesta("D", "4,5", false));
    }

    /**
     * Constructor de contenido que parte de la pregunta válida de ejemplo.
     *
     * @return constructor con valores válidos
     */
    public static ConstructorDeContenido contenido() {
        return new ConstructorDeContenido();
    }

    /**
     * Contenido que supera la validación estructural.
     *
     * @return contenido válido
     */
    public static ContenidoDePregunta contenidoValido() {
        return contenido().construir();
    }

    /**
     * Contenido incompleto (sin contexto), que deja la Pregunta en {@code BORRADOR}.
     *
     * @return contenido inválido
     */
    public static ContenidoDePregunta contenidoIncompleto() {
        return contenido().conContexto("").construir();
    }

    /**
     * Pregunta del Autor de prueba en el estado indicado, construida con {@code reconstituir}.
     *
     * @param estado estado deseado
     * @return la Pregunta
     */
    public static Pregunta preguntaEnEstado(EstadoPregunta estado) {
        ContenidoDePregunta contenido = estado == EstadoPregunta.BORRADOR ? contenidoIncompleto() : contenidoValido();
        return Pregunta.reconstituir(PreguntaId.generar(), AUTOR, contenido, estado, List.of(),
                HistorialDeRevisiones.vacio(), FECHA, FECHA);
    }

    /**
     * Pregunta válida recién creada por el Autor ({@code EN_CONSTRUCCION}), con los eventos ya extraídos.
     *
     * @return la Pregunta
     */
    public static Pregunta preguntaEnConstruccion() {
        Pregunta pregunta = Pregunta.crear(PreguntaId.generar(), AUTOR, contenidoValido(), FECHA);
        pregunta.extraerEventos();
        return pregunta;
    }

    /**
     * Pregunta {@code EN_REVISION}, a la que se llegó por los métodos del lenguaje ubicuo.
     *
     * @return la Pregunta
     */
    public static Pregunta preguntaEnRevision() {
        Pregunta pregunta = preguntaEnConstruccion();
        pregunta.enviarARevision(AUTOR, fecha(1));
        pregunta.iniciarRevision(ADMINISTRADOR, fecha(2));
        pregunta.extraerEventos();
        return pregunta;
    }

    /**
     * Los tres criterios obligatorios con una valoración de 4.
     *
     * @return criterios válidos
     */
    public static List<CriterioEvaluado> criteriosValidos() {
        return List.of(
                new CriterioEvaluado(TipoCriterio.PEDAGOGICO, 4),
                new CriterioEvaluado(TipoCriterio.TECNICO, 5),
                new CriterioEvaluado(TipoCriterio.ESTRUCTURAL, 4));
    }

    /**
     * Una observación de ejemplo.
     *
     * @return lista con una observación
     */
    public static List<Observacion> observaciones() {
        return List.of(new Observacion("El distractor B es poco plausible"));
    }

    /**
     * Constructor fluido de {@link ContenidoDePregunta} para variar un componente a la vez.
     */
    public static final class ConstructorDeContenido {

        private String contexto = "Un grupo de 5 estudiantes obtuvo las notas 3,0; 3,5; 4,0; 4,0 y 4,5.";
        private String preguntaDirecta = "¿Cuál es la moda del conjunto de notas?";
        private List<OpcionDeRespuesta> opciones = opcionesValidas();
        private String justificacion = "La moda es el valor que más se repite: 4,0 aparece dos veces.";
        private List<String> bibliografia = List.of("Walpole, R. Probabilidad y estadística. Pearson, 2012.");
        private ClasificacionAcademica clasificacion = CLASIFICACION;
        private NivelDeDificultad nivel = NivelDeDificultad.BAJO;

        /**
         * Cambia el contexto.
         *
         * @param valor texto nuevo
         * @return este constructor
         */
        public ConstructorDeContenido conContexto(String valor) {
            this.contexto = valor;
            return this;
        }

        /**
         * Cambia la pregunta directa.
         *
         * @param valor texto nuevo
         * @return este constructor
         */
        public ConstructorDeContenido conPreguntaDirecta(String valor) {
            this.preguntaDirecta = valor;
            return this;
        }

        /**
         * Cambia las opciones.
         *
         * @param valor opciones nuevas
         * @return este constructor
         */
        public ConstructorDeContenido conOpciones(List<OpcionDeRespuesta> valor) {
            this.opciones = valor;
            return this;
        }

        /**
         * Cambia la justificación.
         *
         * @param valor texto nuevo
         * @return este constructor
         */
        public ConstructorDeContenido conJustificacion(String valor) {
            this.justificacion = valor;
            return this;
        }

        /**
         * Cambia la bibliografía.
         *
         * @param valor referencias nuevas
         * @return este constructor
         */
        public ConstructorDeContenido conBibliografia(List<String> valor) {
            this.bibliografia = valor;
            return this;
        }

        /**
         * Cambia la clasificación.
         *
         * @param valor clasificación nueva
         * @return este constructor
         */
        public ConstructorDeContenido conClasificacion(ClasificacionAcademica valor) {
            this.clasificacion = valor;
            return this;
        }

        /**
         * Construye el contenido.
         *
         * @return contenido de la Pregunta
         */
        public ContenidoDePregunta construir() {
            return new ContenidoDePregunta(new Contexto(contexto), new PreguntaDirecta(preguntaDirecta), opciones,
                    new Justificacion(justificacion), new Bibliografia(bibliografia), clasificacion, nivel);
        }
    }
}
