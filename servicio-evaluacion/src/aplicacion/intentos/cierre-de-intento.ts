import { IntentoDeSimulacro } from '../../dominio/intentos/intento-de-simulacro';
import { IntentoDeSimulacroRepositorio } from '../../dominio/intentos/intento-de-simulacro.repositorio';
import { PreguntaEvaluableRepositorio } from '../../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { CalificadorSimulacroServicio } from '../../dominio/servicios/calificador-simulacro.servicio';
import { PublicadorEventosPuerto } from '../puertos/salida/publicador-eventos.puerto';

/**
 * Colaborador compartido por los casos de uso que operan sobre un
 * IntentoDeSimulacro (`RegistrarRespuestaCasoUso`, `FinalizarIntentoCasoUso`
 * y `ObtenerIntentoCasoUso`). No es un caso de uso en si mismo: no tiene
 * puerto de entrada propio.
 *
 * Centraliza la aplicacion del vencimiento perezoso (CONTRATOS.md 11.3) y
 * la calificacion automatica que le sigue cuando corresponde (CU-14 + CU-15
 * en el mismo caso de uso, MODELO-DOMINIO.md A.3).
 */
export class CierreDeIntento {
  constructor(
    private readonly preguntaEvaluableRepositorio: PreguntaEvaluableRepositorio,
    private readonly intentoDeSimulacroRepositorio: IntentoDeSimulacroRepositorio,
    private readonly calificador: CalificadorSimulacroServicio,
    private readonly publicadorEventos: PublicadorEventosPuerto,
  ) {}

  /**
   * Aplica el vencimiento perezoso sobre el intento y, si justo por esta
   * llamada quedo FINALIZADO, lo califica, lo guarda y publica sus
   * eventos de inmediato, antes de que el caso de uso que la invoco siga
   * su propio flujo.
   *
   * @param intento El intento a poner al dia.
   * @param ahora Instante de la operacion (RelojPuerto).
   */
  public async ponerAlDia(intento: IntentoDeSimulacro, ahora: Date): Promise<void> {
    const seVencioAhora = intento.aplicarVencimientoPerezoso(ahora);
    if (seVencioAhora) {
      await this.calificarGuardarYPublicar(intento);
    }
  }

  /**
   * Califica un intento ya FINALIZADO, lo cierra con su calificacion, lo
   * guarda y publica los eventos extraidos (entre ellos,
   * `IntentoDeSimulacroCalificado`, el unico que sale al broker).
   *
   * @param intento El intento ya FINALIZADO.
   */
  public async calificarGuardarYPublicar(intento: IntentoDeSimulacro): Promise<void> {
    const preguntas = await this.preguntaEvaluableRepositorio.obtenerPorIds(intento.preguntaIds());
    const calificacion = this.calificador.calificar(intento, preguntas);
    intento.cerrarConCalificacion(calificacion);

    await this.intentoDeSimulacroRepositorio.guardar(intento);
    await this.publicadorEventos.publicar(intento.extraerEventos());
  }
}
