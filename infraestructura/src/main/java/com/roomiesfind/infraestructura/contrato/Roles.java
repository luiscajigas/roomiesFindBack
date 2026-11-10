package com.roomiesfind.infraestructura.contrato;

/** Roles de una cuenta. Un servicio que aún no conoce el rol responde vacío: se trata como usuario. */
public final class Roles {

  public static final String USUARIO = "usuario";
  public static final String ADMIN = "admin";

  private Roles() {}

  public static String normalizar(String rol) {
    return ADMIN.equals(rol) ? ADMIN : USUARIO;
  }
}
