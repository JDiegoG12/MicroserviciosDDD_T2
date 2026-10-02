package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.TipoCriterio;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Fila de {@code formato_criterio} o {@code pregunta_historial_criterio}: un CriterioEvaluado.
 */
@Embeddable
public class CriterioEmbebido {

    @Enumerated(EnumType.STRING)
    @Column(name = "criterio", nullable = false, length = 15)
    private TipoCriterio criterio;

    @Column(name = "valoracion", nullable = false)
    private int valoracion;

    /**
     * Constructor que exige JPA.
     */
    protected CriterioEmbebido() {
    }

    /**
     * Crea la fila.
     *
     * @param criterio   criterio
     * @param valoracion valoración de 1 a 5
     */
    public CriterioEmbebido(TipoCriterio criterio, int valoracion) {
        this.criterio = criterio;
        this.valoracion = valoracion;
    }

    /** @return criterio */
    public TipoCriterio getCriterio() {
        return criterio;
    }

    /** @return valoración */
    public int getValoracion() {
        return valoracion;
    }
}
