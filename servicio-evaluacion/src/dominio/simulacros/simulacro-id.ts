import { esUuidValido, generarUuid } from '../compartido/uuid';
import { SolicitudInvalidaExcepcion } from '../excepciones/solicitud-invalida.excepcion';

/**
 * Identificador de un Simulacro, siempre un UUID v4 en texto
 * (CONTRATOS.md seccion 4).
 */
export class SimulacroId {
  private constructor(private readonly valor: string) {}

  /**
   * Genera un SimulacroId nuevo.
   *
   * @returns Un identificador nuevo.
   */
  public static generar(): SimulacroId {
    return new SimulacroId(generarUuid());
  }

  /**
   * Construye un SimulacroId a partir de un texto existente, por ejemplo
   * el parametro de ruta `simulacroId` de una peticion.
   *
   * @param texto Texto a validar.
   * @returns El identificador construido.
   * @throws SolicitudInvalidaExcepcion Si `texto` no es un UUID valido.
   */
  public static desde(texto: string): SimulacroId {
    if (!esUuidValido(texto)) {
      throw new SolicitudInvalidaExcepcion('El simulacroId debe ser un UUID valido.');
    }
    return new SimulacroId(texto);
  }

  /**
   * Representacion en texto del identificador.
   *
   * @returns El UUID en texto.
   */
  public aTexto(): string {
    return this.valor;
  }

  /**
   * Compara este identificador con otro por su valor.
   *
   * @param otro El otro SimulacroId.
   * @returns `true` si representan el mismo UUID.
   */
  public igualA(otro: SimulacroId): boolean {
    return this.valor === otro.valor;
  }
}
