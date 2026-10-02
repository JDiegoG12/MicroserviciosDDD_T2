package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConversorDeComandos;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.DatosDePreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.CrearPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.CatalogoAcademicoPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.MapeadorDeResultados;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ContenidoDePregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;

/**
 * CU-04, Crear pregunta. Rol {@code AUTOR}. Endpoint futuro: {@code POST /preguntas} (201).
 *
 * <p>Valida la clasificación con Catálogo <strong>antes</strong> de guardar: si es inválida o Catálogo no
 * responde, no se guarda nada (CONTRATOS.md 6).</p>
 */
public final class CrearPreguntaCasoUso implements CrearPregunta {

    private final PreguntaRepositorio preguntaRepositorio;
    private final VerificadorDeClasificacion verificadorDeClasificacion;
    private final PublicadorEventosPuerto publicadorEventos;
    private final RelojPuerto reloj;

    /**
     * Crea el caso de uso con sus puertos.
     *
     * @param preguntaRepositorio repositorio de Preguntas
     * @param catalogo            puerto hacia Catálogo
     * @param publicadorEventos   publicador de eventos
     * @param reloj               fuente de la hora
     */
    public CrearPreguntaCasoUso(PreguntaRepositorio preguntaRepositorio, CatalogoAcademicoPuerto catalogo,
                                PublicadorEventosPuerto publicadorEventos, RelojPuerto reloj) {
        this.preguntaRepositorio = preguntaRepositorio;
        this.verificadorDeClasificacion = new VerificadorDeClasificacion(catalogo);
        this.publicadorEventos = publicadorEventos;
        this.reloj = reloj;
    }

    /**
     * {@inheritDoc}
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion             si no tiene el rol {@code AUTOR}
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion               si falta la clasificación o el nivel
     * @throws co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.ClasificacionInvalidaExcepcion  si Catálogo rechaza la clasificación
     * @throws co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.CatalogoNoDisponibleExcepcion   si Catálogo no responde
     */
    @Override
    public PreguntaRespuesta ejecutar(UsuarioActual usuario, DatosDePreguntaComando datos) {
        usuario.exigirAlgunRol(Rol.AUTOR);
        ContenidoDePregunta contenido = ConversorDeComandos.aContenido(datos);
        verificadorDeClasificacion.exigirValida(contenido.clasificacion());

        Pregunta pregunta = Pregunta.crear(PreguntaId.generar(), usuario.id(), contenido, reloj.ahora());
        preguntaRepositorio.guardar(pregunta);
        publicadorEventos.publicar(pregunta.extraerEventos());
        return MapeadorDeResultados.aPreguntaRespuesta(pregunta);
    }
}
