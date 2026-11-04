package com.roomiesfind.ia.model;

/** Un proveedor externo de IA, al que se llega por REST. */
public interface ProveedorIa {

  String nombre();

  boolean estaConfigurado();

  /** @throws RuntimeException si falla; el mensaje nunca incluye la clave de API */
  String generar(String prompt);
}
