import {
  ClasificacionDePregunta,
  OpcionDePregunta,
} from '../../../dominio/preguntas-evaluables/contenido-de-pregunta';

/**
 * Comando de entrada de `RegistrarPreguntaPublicadaCasoUso`, construido por
 * el futuro consumidor de RabbitMQ a partir del evento `PreguntaPublicada`
 * (CONTRATOS.md 7.4).
 */
export interface RegistrarPreguntaPublicadaComando {
  readonly idEvento: string;
  readonly preguntaId: string;
  readonly contexto: string;
  readonly preguntaDirecta: string;
  readonly opciones: readonly OpcionDePregunta[];
  readonly letraCorrecta: string;
  readonly clasificacion: ClasificacionDePregunta;
  readonly nivelDificultad: string;
  readonly fechaPublicacion: Date;
}
