package com.roomiesfind.usuarios.model;

/** Fila de la tabla usuarios. */
public record Usuario(int id, String nombre, String email, String passwordHash, String rol) {

  /** Nunca incluye el hash, para que no termine en los logs. */
  @Override
  public String toString() {
    return "Usuario[id=" + id + "]";
  }
}
