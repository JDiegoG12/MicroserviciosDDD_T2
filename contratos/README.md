# Contratos compartidos

Esta carpeta guarda los **contratos de integración** entre los tres microservicios: el contrato gRPC del Catálogo Académico y los esquemas de los eventos que viajan por RabbitMQ. Se construyen a partir de [CONTRATOS.md](../CONTRATOS.md) (secciones 6 y 7), que es la fuente única de verdad.

## Contenido y dueños

| Archivo | Qué es | Dueño | Sección de CONTRATOS.md |
|---|---|---|---|
| `proto/catalogo/v1/catalogo_academico.proto` | Servicio gRPC `CatalogoAcademico` (`ValidarClasificacion`), paquete `bancopreguntas.catalogo.v1` | P3 (`servicio-catalogo`) | 6 |
| `eventos/editorial/pregunta-publicada.v1.schema.json` | JSON Schema (draft 2020-12) del evento `PreguntaPublicada` | P1 (`servicio-editorial`) | 7.3 y 7.4 |
| `eventos/editorial/pregunta-archivada.v1.schema.json` | JSON Schema (draft 2020-12) del evento `PreguntaArchivada` | P1 (`servicio-editorial`) | 7.3 y 7.5 |
| `eventos/evaluacion/intento-calificado.v1.schema.json` | JSON Schema (draft 2020-12) del evento `IntentoDeSimulacroCalificado` | P2 (`servicio-evaluacion`) | 7.3 y 7.6 |
| `eventos/ejemplos/*.v1.ejemplo.json` | Un ejemplo válido por evento (mismo nombre base que su esquema) | dueño del evento | 7.8 |
| `eventos/ejemplos/pregunta-publicada.v1.ejemplo-campo-extra.json` | El ejemplo de `PreguntaPublicada` con un campo `campoFuturo` adicional en `datos`: el esquema (estricto) debe RECHAZARLO; el consumidor de Evaluación, en cambio, debe procesarlo sin mandarlo a la DLQ | P1 (`servicio-editorial`) | 7.7.4 y 7.8 |

## Cómo se usan

- **gRPC:** cada servicio genera su código desde este `.proto` durante la construcción (Java con `protobuf-maven-plugin`, Python con `grpcio-tools`). El código generado **nunca** se versiona y el `.proto` **no** se copia ni se edita dentro de los servicios. Por eso el contexto de Docker es la raíz del repositorio.
- **Eventos:** los esquemas describen exactamente lo que el productor debe **escribir** (por eso usan `additionalProperties: false`). El consumidor es tolerante (CONTRATOS.md §7.7.4): ignora campos desconocidos y **no** valida con estos esquemas estrictos; en sus pruebas usa los ejemplos normales de `eventos/ejemplos/` más el ejemplo con campo extra, que debe aceptar igual.

## Cómo validar

Solo se necesita Docker:

```bash
./scripts/validar-contratos.sh                                            # Linux, macOS o Git Bash
powershell -ExecutionPolicy Bypass -File .\scripts\validar-contratos.ps1  # Windows
```

El script compila el `.proto`, comprueba que cada esquema sea draft 2020-12 válido, valida cada ejemplo contra su esquema y comprueba que el ejemplo con campo extra sea rechazado por el esquema estricto.

## Cómo se cambian

**Cualquier cambio en esta carpeta requiere un PR aprobado por al menos otro integrante** (ver `.github/CODEOWNERS`; no se exige la aprobación de los tres) y debe modificar `CONTRATOS.md` y `/contratos` en el **mismo commit**, avisando al equipo. Un cambio incompatible en un evento no edita la versión existente: crea la versión `2` (`*.v2.schema.json`) acordada entre los tres.
