package co.edu.unicauca.bancopreguntas.editorial.fabricas;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Valida JSON contra los esquemas estrictos de {@code /contratos/eventos} (CONTRATOS.md 7.8). La ruta llega por la
 * propiedad de sistema {@code directorio.esquemas.eventos} que fija el pom.xml.
 */
public final class ValidadorDeContratos {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ValidadorDeContratos() {
    }

    /**
     * Valida un JSON contra un esquema de eventos.
     *
     * @param rutaRelativa ruta dentro de {@code contratos/eventos}, por ejemplo {@code editorial/pregunta-publicada.v1.schema.json}
     * @param json         JSON a validar
     * @return errores encontrados (vacía si cumple)
     */
    public static List<String> validar(String rutaRelativa, byte[] json) {
        Path esquema = Path.of(System.getProperty("directorio.esquemas.eventos", "../contratos/eventos"), rutaRelativa);
        try (InputStream contenido = Files.newInputStream(esquema)) {
            Schema validador = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(contenido);
            JsonNode documento = JSON.readTree(json);
            return validador.validate(documento).stream().map(Error::toString).toList();
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo leer el esquema " + esquema, error);
        }
    }
}
