# Arquitectura · Banco de Preguntas Saber Pro (Taller 2)

Esta carpeta contiene el **diagrama de arquitectura** del sistema, que es un entregable del taller (responsable: P3).

> TODO: reemplazar o complementar el diagrama de partida con la versión final del entregable (por ejemplo, exportada como imagen o PDF y guardada en esta carpeta).

## Diagrama de partida

Tomado de [CONTRATOS.md](../../CONTRATOS.md), sección 1. Si el contrato cambia, este diagrama debe actualizarse en el mismo PR.

```mermaid
flowchart LR
  PM(["Postman"])
  ED["servicio-editorial<br/>Java · Spring Boot<br/>:8081"]
  CA["servicio-catalogo<br/>Python · FastAPI<br/>:8082 REST · :50051 gRPC"]
  EV["servicio-evaluacion<br/>TypeScript · NestJS<br/>:8083"]
  EX1{{"exchange editorial.eventos<br/>(topic)"}}
  Q1[["cola evaluacion.preguntas"]]
  EX2{{"exchange evaluacion.eventos<br/>(topic)"}}
  DB1[("bd-editorial<br/>PostgreSQL")]
  DB2[("bd-catalogo<br/>PostgreSQL")]
  DB3[("bd-evaluacion<br/>MongoDB")]

  PM -- "REST /api/v1" --> ED
  PM -- "REST /api/v1" --> CA
  PM -- "REST /api/v1" --> EV
  ED -- "gRPC CatalogoAcademico.ValidarClasificacion" --> CA
  ED -- "pregunta.publicada<br/>pregunta.archivada" --> EX1
  EX1 --> Q1
  Q1 -- "consume" --> EV
  EV -- "intento.calificado<br/>(sin consumidor por ahora)" --> EX2
  ED --- DB1
  CA --- DB2
  EV --- DB3
```

## Comunicaciones del sistema

| # | Tipo | Origen → Destino | Contrato |
|---|---|---|---|
| C1 | REST | Postman → cada servicio | APIs `/api/v1/...` (CONTRATOS.md §8) |
| C2 | gRPC síncrono | `servicio-editorial` → `servicio-catalogo` | `CatalogoAcademico.ValidarClasificacion` (§6) |
| C3 | Evento asíncrono | `servicio-editorial` → RabbitMQ → `servicio-evaluacion` | `PreguntaPublicada`, `PreguntaArchivada` (§7) |
| C4 | Evento asíncrono | `servicio-evaluacion` → RabbitMQ | `IntentoDeSimulacroCalificado`, sin consumidor (§7) |

Ningún servicio lee la base de datos de otro y ningún servicio llama por REST a otro servicio.
