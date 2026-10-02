#!/usr/bin/env node
'use strict';

/**
 * Publica en RabbitMQ (via la API HTTP de administracion) los eventos de
 * ejemplo de `/contratos/eventos/ejemplos/`, mas varias variaciones de
 * `PreguntaPublicada` con `idEvento` y `preguntaId` distintos, para tener
 * varias candidatas al definir un Simulacro.
 *
 * Uso: node scripts/publicar-ejemplos.js
 * Variables de entorno (todas opcionales, con los valores por defecto
 * locales de CONTRATOS.md 9.2):
 *   RABBITMQ_HOST          (por defecto localhost)
 *   RABBITMQ_MGMT_PUERTO   (por defecto 15672, puerto de la consola web)
 *   RABBITMQ_USUARIO       (por defecto banco)
 *   RABBITMQ_CLAVE         (por defecto banco123)
 */

const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const HOST = process.env.RABBITMQ_HOST ?? 'localhost';
const PUERTO_ADMIN = process.env.RABBITMQ_MGMT_PUERTO ?? '15672';
const USUARIO = process.env.RABBITMQ_USUARIO ?? 'banco';
const CLAVE = process.env.RABBITMQ_CLAVE ?? 'banco123';
const URL_PUBLICAR = `http://${HOST}:${PUERTO_ADMIN}/api/exchanges/%2F/editorial.eventos/publish`;

const DIR_EJEMPLOS = path.resolve(__dirname, '..', '..', 'contratos', 'eventos', 'ejemplos');

function leerEjemplo(nombreArchivo) {
  const ruta = path.join(DIR_EJEMPLOS, nombreArchivo);
  return JSON.parse(fs.readFileSync(ruta, 'utf-8'));
}

async function publicar(routingKey, evento) {
  const cuerpo = {
    properties: {
      content_type: 'application/json',
      content_encoding: 'utf-8',
      delivery_mode: 2,
      message_id: evento.idEvento,
      type: evento.tipoEvento,
    },
    routing_key: routingKey,
    payload: JSON.stringify(evento),
    payload_encoding: 'string',
  };

  const credenciales = Buffer.from(`${USUARIO}:${CLAVE}`).toString('base64');
  const respuesta = await fetch(URL_PUBLICAR, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Basic ${credenciales}`,
    },
    body: JSON.stringify(cuerpo),
  });

  const texto = await respuesta.text();
  const estado = respuesta.ok ? 'OK' : `ERROR ${respuesta.status}`;
  console.log(
    `[${estado}] ${routingKey} idEvento=${evento.idEvento} preguntaId=${evento.datos.preguntaId} -> ${texto}`,
  );
  if (!respuesta.ok) {
    throw new Error(`No se pudo publicar en RabbitMQ (${respuesta.status}): ${texto}`);
  }
}

/**
 * Construye una variacion de `PreguntaPublicada` con un `idEvento` y un
 * `preguntaId` nuevos, para tener varias candidatas distintas al definir
 * un Simulacro.
 */
function variarPreguntaPublicada(base, indice) {
  const clasificaciones = [
    { competenciaId: '22222222-2222-4222-8222-000000000101', temaId: '22222222-2222-4222-8222-000000000201', subtemaId: '22222222-2222-4222-8222-000000000301' },
    { competenciaId: '22222222-2222-4222-8222-000000000101', temaId: '22222222-2222-4222-8222-000000000201', subtemaId: '22222222-2222-4222-8222-000000000302' },
    { competenciaId: '22222222-2222-4222-8222-000000000101', temaId: '22222222-2222-4222-8222-000000000202', subtemaId: '22222222-2222-4222-8222-000000000303' },
    { competenciaId: '22222222-2222-4222-8222-000000000102', temaId: '22222222-2222-4222-8222-000000000203', subtemaId: '22222222-2222-4222-8222-000000000304' },
  ];
  const niveles = ['BAJO', 'MEDIO', 'ALTO'];

  return {
    ...base,
    idEvento: crypto.randomUUID(),
    idCorrelacion: crypto.randomUUID(),
    datos: {
      ...base.datos,
      preguntaId: crypto.randomUUID(),
      preguntaDirecta: `${base.datos.preguntaDirecta} (variacion ${indice})`,
      clasificacion: clasificaciones[indice % clasificaciones.length],
      nivelDificultad: niveles[indice % niveles.length],
    },
  };
}

async function main() {
  const publicadaBase = leerEjemplo('pregunta-publicada.v1.ejemplo.json');
  const publicadaCampoExtra = leerEjemplo('pregunta-publicada.v1.ejemplo-campo-extra.json');
  const archivadaBase = leerEjemplo('pregunta-archivada.v1.ejemplo.json');

  console.log(`Publicando en ${URL_PUBLICAR} ...`);

  // El ejemplo base y el de campo extra (demuestra el lector tolerante,
  // CONTRATOS.md 7.7.4).
  await publicar('pregunta.publicada', publicadaBase);
  await publicar('pregunta.publicada', publicadaCampoExtra);

  // Variaciones con preguntaId e idEvento distintos, para tener varias
  // candidatas publicadas al definir un Simulacro.
  for (let i = 0; i < 4; i += 1) {
    await publicar('pregunta.publicada', variarPreguntaPublicada(publicadaBase, i));
  }

  // Un evento de archivado de ejemplo (de una pregunta que este servicio
  // aun no conoce: queda como marca de archivo, CONTRATOS.md 7.7.3).
  await publicar('pregunta.archivada', archivadaBase);

  console.log('Listo.');
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
