package co.edu.unicauca.bancopreguntas.editorial.dobles;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.CatalogoNoDisponibleExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.CatalogoAcademicoPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.MotivoRechazoClasificacion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.ResultadoValidacionClasificacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ClasificacionAcademica;

/**
 * Doble de {@link CatalogoAcademicoPuerto} con tres comportamientos: válido, inválido o caído.
 */
public final class CatalogoAcademicoFalso implements CatalogoAcademicoPuerto {

    /** Comportamiento del doble. */
    public enum Modo {
        /** Acepta cualquier clasificación. */
        VALIDO,
        /** Rechaza con {@code SUBTEMA_NO_PERTENECE_A_TEMA}. */
        INVALIDO,
        /** Simula que Catálogo no responde. */
        CAIDO
    }

    /** Detalle que devuelve en modo inválido. */
    public static final String DETALLE_INVALIDO = "El subtema no pertenece al tema indicado.";

    private Modo modo = Modo.VALIDO;
    private int cantidadDeLlamadas;

    /**
     * Cambia el comportamiento.
     *
     * @param nuevoModo modo nuevo
     */
    public void cambiarA(Modo nuevoModo) {
        this.modo = nuevoModo;
    }

    @Override
    public ResultadoValidacionClasificacion validarClasificacion(ClasificacionAcademica clasificacion) {
        cantidadDeLlamadas++;
        return switch (modo) {
            case VALIDO -> ResultadoValidacionClasificacion.valido();
            case INVALIDO -> ResultadoValidacionClasificacion.invalido(
                    MotivoRechazoClasificacion.SUBTEMA_NO_PERTENECE_A_TEMA, DETALLE_INVALIDO);
            case CAIDO -> throw new CatalogoNoDisponibleExcepcion("Catálogo no respondió en 2 s.", null);
        };
    }

    /**
     * Cuántas veces se consultó el catálogo.
     *
     * @return cantidad de llamadas
     */
    public int cantidadDeLlamadas() {
        return cantidadDeLlamadas;
    }
}
