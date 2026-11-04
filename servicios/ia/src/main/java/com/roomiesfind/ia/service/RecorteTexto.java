package com.roomiesfind.ia.service;

import java.util.regex.Pattern;

/** Evita mostrar frases incompletas si el proveedor cortó el texto. */
final class RecorteTexto {

  private static final Pattern TERMINA_EN_ORACION = Pattern.compile("[.!?…][\"'”)]?\\s*$");
  private static final int LARGO_MINIMO = 40;

  private RecorteTexto() {}

  static String hastaUltimaOracionCompleta(String texto) {
    if (TERMINA_EN_ORACION.matcher(texto).find()) return texto;
    int corte = Math.max(texto.lastIndexOf('.'), Math.max(texto.lastIndexOf('!'), texto.lastIndexOf('?')));
    return corte > LARGO_MINIMO ? texto.substring(0, corte + 1).trim() : texto;
  }
}
