package com.roomiesfind.comun;

/** Lee la configuración del entorno. Conserva NODE_ENV para no cambiar las variables ya configuradas. */
public final class Entorno {

  private Entorno() {}

  public static boolean esProduccion() {
    return "production".equals(System.getenv("NODE_ENV"));
  }

  public static String obligatoria(String nombre) {
    String valor = System.getenv(nombre);
    if (valor == null || valor.isBlank()) {
      throw new IllegalStateException(nombre + " es obligatoria.");
    }
    return valor;
  }

  public static String opcional(String nombre, String porDefecto) {
    String valor = System.getenv(nombre);
    return valor == null || valor.isBlank() ? porDefecto : valor;
  }
}
