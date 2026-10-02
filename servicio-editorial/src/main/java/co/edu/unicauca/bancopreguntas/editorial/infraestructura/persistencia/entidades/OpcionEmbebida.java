package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Fila de {@code pregunta_opcion}: una Opción de respuesta.
 */
@Embeddable
public class OpcionEmbebida {

    @Column(name = "letra", nullable = false, columnDefinition = "text")
    private String letra;

    @Column(name = "texto", nullable = false, columnDefinition = "text")
    private String texto;

    @Column(name = "es_correcta", nullable = false)
    private boolean esCorrecta;

    /**
     * Constructor que exige JPA.
     */
    protected OpcionEmbebida() {
    }

    /**
     * Crea la fila.
     *
     * @param letra      letra
     * @param texto      texto
     * @param esCorrecta si es la Respuesta correcta
     */
    public OpcionEmbebida(String letra, String texto, boolean esCorrecta) {
        this.letra = letra;
        this.texto = texto;
        this.esCorrecta = esCorrecta;
    }

    /** @return letra */
    public String getLetra() {
        return letra;
    }

    /** @return texto */
    public String getTexto() {
        return texto;
    }

    /** @return si es la Respuesta correcta */
    public boolean isEsCorrecta() {
        return esCorrecta;
    }
}
