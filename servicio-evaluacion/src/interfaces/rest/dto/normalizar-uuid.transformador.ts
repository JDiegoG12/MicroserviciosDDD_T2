import { Transform } from 'class-transformer';

/**
 * Normaliza un campo UUID a minusculas antes de validarlo con `@IsUUID`
 * (CONTRATOS.md seccion 4: un UUID canonico en mayusculas se acepta y se
 * normaliza). Se aplica antes de `@IsUUID` porque `ValidationPipe`
 * ejecuta las transformaciones de `class-transformer` antes de validar.
 */
export function NormalizarUuid(): PropertyDecorator {
  return Transform(({ value }: { value: unknown }) => (typeof value === 'string' ? value.toLowerCase() : value));
}

/** Igual que `NormalizarUuid`, pero para un arreglo de UUID (`@IsUUID('4', { each: true })`). */
export function NormalizarUuidLista(): PropertyDecorator {
  return Transform(({ value }: { value: unknown }) =>
    Array.isArray(value) ? value.map((elemento) => (typeof elemento === 'string' ? elemento.toLowerCase() : elemento)) : value,
  );
}
