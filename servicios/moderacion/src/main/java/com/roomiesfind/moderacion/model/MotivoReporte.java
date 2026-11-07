package com.roomiesfind.moderacion.model;

import java.util.Arrays;

/** Debe coincidir con la restricción de la tabla reportes. */
public enum MotivoReporte {
  ACOSO("acoso"),
  PERFIL_FALSO("perfil_falso"),
  ESTAFA("estafa"),
  CONTENIDO_INAPROPIADO("contenido_inapropiado"),
  OTRO("otro");

  private final String valor;

  MotivoReporte(String valor) {
    this.valor = valor;
  }

  public String valor() {
    return valor;
  }

  public static boolean existe(String valor) {
    return Arrays.stream(values()).anyMatch((motivo) -> motivo.valor.equals(valor));
  }
}
