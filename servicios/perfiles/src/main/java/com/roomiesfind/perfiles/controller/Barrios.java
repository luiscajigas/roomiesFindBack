package com.roomiesfind.perfiles.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Barrios de Pasto que acepta el sistema. El archivo barrios-pasto.txt debe
 * coincidir con la lista del frontend (src/app/data/barrios-pasto.ts).
 */
@Component
public class Barrios {

  private final Set<String> barrios;

  public Barrios() {
    try (InputStream archivo = Barrios.class.getResourceAsStream("/barrios-pasto.txt")) {
      barrios = new String(archivo.readAllBytes(), StandardCharsets.UTF_8).lines()
          .map(String::trim)
          .filter((linea) -> !linea.isEmpty() && !linea.startsWith("#"))
          .collect(Collectors.toUnmodifiableSet());
    } catch (IOException error) {
      throw new UncheckedIOException(error);
    }
  }

  public boolean esValido(String zona) {
    return zona != null && barrios.contains(zona);
  }

  public int total() {
    return barrios.size();
  }
}
