import { esUuidValido, generarUuid } from '../compartido/uuid';
import { SolicitudInvalidaExcepcion } from '../excepciones/solicitud-invalida.excepcion';

/**
 * Identificador de un IntentoDeSimulacro, siempre un UUID v4 en texto
 * (CONTRATOS.md seccion 4).
 */
export class IntentoId {
  private constructor(private readonly valor: string) {}

  /**
   * Genera un IntentoId nuevo.
   *
   * @returns Un identificador nuevo.
   */
  public static generar(): IntentoId {
    return new IntentoId(generarUuid());
  }

  /**
   * Construye un IntentoId a partir de un texto existente, por ejemplo el
   * parametro de ruta `intentoId` de una peticion.
   *
   * @param texto Texto a validar.
   * @returns El identificador construido.
   * @throws SolicitudInvalidaExcepcion Si `texto` no es un UUID valido.
   */
  public static desde(texto: string): IntentoId {
    if (!esUuidValido(texto)) {
      throw new SolicitudInvalidaExcepcion('El intentoId debe ser un UUID valido.');
    }
    return new IntentoId(texto);
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
   * @param otro El otro IntentoId.
   * @returns `true` si representan el mismo UUID.
   */
  public igualA(otro: IntentoId): boolean {
    return this.valor === otro.valor;
  }
}
