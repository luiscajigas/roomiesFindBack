package com.roomiesfind.infraestructura.rest;

import java.math.BigDecimal;
import org.springframework.http.HttpStatus;

/** Lee los parámetros de ruta y de consulta. Lo que no es válido se responde con 400. */
public final class Parametros {

  private static final String INVALIDA = "La solicitud no es válida.";

  private Parametros() {}

  public static boolean tieneTexto(String valor) {
    return valor != null && !valor.isBlank();
  }

  public static int entero(String texto) {
    try {
      return Integer.parseInt(texto.trim());
    } catch (NumberFormatException error) {
      throw invalida();
    }
  }

  /** Un id mayor que cero. */
  public static int id(String texto) {
    long id = idLargo(texto);
    if (id > Integer.MAX_VALUE) throw invalida();
    return (int) id;
  }

  public static long idLargo(String texto) {
    try {
      long id = Long.parseLong(texto);
      if (id > 0) return id;
    } catch (NumberFormatException error) {
      // Se responde abajo como solicitud inválida.
    }
    throw invalida();
  }

  /**
   * Como {@link #id}, pero un número entero válido y enorme se toma como un id que no existe
   * (responde 404 y no 400), igual que en el backend original.
   */
  public static int idTolerante(String texto) {
    try {
      BigDecimal id = new BigDecimal(texto);
      if (id.signum() > 0 && id.stripTrailingZeros().scale() <= 0) {
        return id.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0 ? Integer.MAX_VALUE : id.intValueExact();
      }
    } catch (NumberFormatException error) {
      // Se responde abajo como identificador inválido.
    }
    throw new ErrorRest(HttpStatus.BAD_REQUEST, "Identificador de perfil inválido.");
  }

  private static ErrorRest invalida() {
    return new ErrorRest(HttpStatus.BAD_REQUEST, INVALIDA);
  }
}
