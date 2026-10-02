package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.CatalogoNoDisponibleExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ClasificacionAcademica;

/**
 * Consulta al Catálogo Académico si una clasificación existe y es coherente (INV-07, D-13). En la etapa 2 lo
 * implementa {@code CatalogoAcademicoGrpcAdaptador} sobre {@code CatalogoAcademico.ValidarClasificacion}
 * (CONTRATOS.md 6).
 */
public interface CatalogoAcademicoPuerto {

    /**
     * Valida la terna Competencia/Tema/Subtema. Una terna inválida no es un error: se responde con
     * {@code valida = false} y el motivo.
     *
     * @param clasificacion terna a validar
     * @return resultado de la validación
     * @throws CatalogoNoDisponibleExcepcion si Catálogo no responde o supera el deadline de 2 s ({@code CATALOGO_NO_DISPONIBLE})
     */
    ResultadoValidacionClasificacion validarClasificacion(ClasificacionAcademica clasificacion);
}
