"""Capa de infraestructura (adaptadores de salida). Vacía en la etapa 1; se llena en la etapa 2.

Aquí irán, según CONTRATOS.md 3.3 y 11.2:

* modelos SQLAlchemy, mappers y ``CompetenciaRepositorioSqlAlchemy`` (implementa
  ``CompetenciaRepositorio`` sobre PostgreSQL ``bd-catalogo``);
* el adaptador de ``PublicadorEventosPuerto`` que solo escribe los eventos en el log
  (Catálogo no publica al broker);
* la configuración (variables ``CATALOGO_*``) y la ejecución de la siembra al arrancar con
  ``SembrarCatalogoCasoUso`` y ``datos_semilla``.
"""
