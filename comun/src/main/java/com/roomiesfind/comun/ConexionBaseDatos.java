package com.roomiesfind.comun;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Convierte DATABASE_URL (postgres://usuario:clave@host/base?sslmode=require,
 * el formato de Neon y Render) en los datos que necesita JDBC.
 */
public record ConexionBaseDatos(String jdbcUrl, String usuario, String clave) {

  public static ConexionBaseDatos desdeEntorno() {
    return desdeUrl(Entorno.obligatoria("DATABASE_URL"), Entorno.esProduccion());
  }

  public static ConexionBaseDatos desdeUrl(String url, boolean produccion) {
    URI uri = URI.create(url);
    String puerto = uri.getPort() > 0 ? ":" + uri.getPort() : "";
    String sslmode = leerSslmode(uri.getRawQuery());
    if (sslmode == null && produccion) sslmode = "require";

    // prepareThreshold=0: compatible con el pooler de conexiones de Neon.
    StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
        .append(uri.getHost()).append(puerto).append(uri.getPath())
        .append("?prepareThreshold=0");
    if (sslmode != null) jdbc.append("&sslmode=").append(sslmode);

    String[] credenciales = uri.getRawUserInfo() == null ? new String[0] : uri.getRawUserInfo().split(":", 2);
    return new ConexionBaseDatos(
        jdbc.toString(),
        credenciales.length > 0 ? decodificar(credenciales[0]) : null,
        credenciales.length > 1 ? decodificar(credenciales[1]) : null);
  }

  private static String leerSslmode(String consulta) {
    if (consulta == null) return null;
    for (String parametro : consulta.split("&")) {
      if (parametro.startsWith("sslmode=")) return parametro.substring("sslmode=".length());
    }
    return null;
  }

  private static String decodificar(String texto) {
    return URLDecoder.decode(texto.replace("+", "%2B"), StandardCharsets.UTF_8);
  }

  /** Nunca incluye la clave, para que no termine en los logs. */
  @Override
  public String toString() {
    return "ConexionBaseDatos[" + jdbcUrl + "]";
  }
}
