package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConversorDeComandos;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ModificarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.PreguntaNoEncontradaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ModificarPregunta;
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
 * CU-05, Modificar pregunta. Rol {@code AUTOR} (la propiedad la verifica el dominio). Endpoint futuro:
 * {@code PUT /preguntas/{preguntaId}} (200).
 *
 * <p>Orden: existencia (404), propiedad y estado editable (403 / 409), clasificación con Catálogo (422 / 503)
 * y, solo entonces, la modificación y el guardado.</p>
 */
public final class ModificarPreguntaCasoUso implements ModificarPregunta {

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
    public ModificarPreguntaCasoUso(PreguntaRepositorio preguntaRepositorio, CatalogoAcademicoPuerto catalogo,
                                    PublicadorEventosPuerto publicadorEventos, RelojPuerto reloj) {
        this.preguntaRepositorio = preguntaRepositorio;
        this.verificadorDeClasificacion = new VerificadorDeClasificacion(catalogo);
        this.publicadorEventos = publicadorEventos;
        this.reloj = reloj;
    }

    /**
     * {@inheritDoc}
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion             sin rol {@code AUTOR} o si no es el autor
     * @throws PreguntaNoEncontradaExcepcion                                                                    si la Pregunta no existe
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.PreguntaNoEditableExcepcion         si no está en estado editable (INV-10)
     * @throws co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.ClasificacionInvalidaExcepcion  si Catálogo rechaza la clasificación
     * @throws co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.CatalogoNoDisponibleExcepcion   si Catálogo no responde
     */
    @Override
    public PreguntaRespuesta ejecutar(UsuarioActual usuario, ModificarPreguntaComando comando) {
        usuario.exigirAlgunRol(Rol.AUTOR);
        Pregunta pregunta = preguntaRepositorio.obtenerPorId(PreguntaId.de(comando.preguntaId()))
                .orElseThrow(() -> new PreguntaNoEncontradaExcepcion(comando.preguntaId()));
        pregunta.exigirQuePuedaModificar(usuario.id());
        ContenidoDePregunta contenido = ConversorDeComandos.aContenido(comando.datos());
        verificadorDeClasificacion.exigirValida(contenido.clasificacion());

        pregunta.modificar(usuario.id(), contenido, reloj.ahora());
        preguntaRepositorio.guardar(pregunta);
        publicadorEventos.publicar(pregunta.extraerEventos());
        return MapeadorDeResultados.aPreguntaRespuesta(pregunta);
    }
}
