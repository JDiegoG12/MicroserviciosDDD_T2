package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.CatalogoNoDisponibleExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.ClasificacionInvalidaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.CatalogoAcademicoPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.ResultadoValidacionClasificacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ClasificacionAcademica;

/**
 * Consulta a Catálogo si la clasificación existe y es coherente, como piden crear y modificar antes de
 * guardar (INV-07; CONTRATOS.md 6).
 */
public final class VerificadorDeClasificacion {

    private final CatalogoAcademicoPuerto catalogo;

    /**
     * Crea el verificador.
     *
     * @param catalogo puerto hacia el Catálogo Académico
     */
    public VerificadorDeClasificacion(CatalogoAcademicoPuerto catalogo) {
        this.catalogo = catalogo;
    }

    /**
     * Exige que Catálogo acepte la clasificación.
     *
     * @param clasificacion terna a validar
     * @throws ClasificacionInvalidaExcepcion si Catálogo la rechaza ({@code CLASIFICACION_INVALIDA}, 422)
     * @throws CatalogoNoDisponibleExcepcion  si Catálogo no responde ({@code CATALOGO_NO_DISPONIBLE}, 503)
     */
    public void exigirValida(ClasificacionAcademica clasificacion) {
        ResultadoValidacionClasificacion resultado = catalogo.validarClasificacion(clasificacion);
        if (!resultado.valida()) {
            throw new ClasificacionInvalidaExcepcion(resultado.motivo().name(), resultado.detalle());
        }
    }
}
