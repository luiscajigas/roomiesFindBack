package com.roomiesfind.perfiles.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.perfiles.dto.DatosPerfilDto;
import com.roomiesfind.perfiles.dto.FotoDto;
import org.junit.jupiter.api.Test;

class ReglasPerfilTest {

  private static DatosPerfilDto datos(String presupuesto, String horario, String limpieza, String ruido, String visitas, String descripcion) {
    return new DatosPerfilDto(presupuesto, null, horario, limpieza, ruido, visitas, null, null, descripcion, null);
  }

  private static String errorDe(DatosPerfilDto datos) {
    return assertThrows(ErrorNegocio.class, () -> ReglasPerfil.validarPreferencias(datos)).getMessage();
  }

  @Test
  void aceptaValoresDeFormularioComoTextoYCamposAusentes() {
    assertDoesNotThrow(() -> ReglasPerfil.validarPreferencias(datos("500000", "mixto", "3", "1", "nunca", "hola")));
    assertDoesNotThrow(() -> ReglasPerfil.validarPreferencias(datos(null, null, null, null, null, null)));
  }

  @Test
  void rechazaPreferenciasFueraDeRango() {
    assertTrue(errorDe(datos("-1", null, null, null, null, null)).contains("presupuesto"));
    assertTrue(errorDe(datos("", null, null, null, null, null)).contains("presupuesto"));
    assertTrue(errorDe(datos("1.5", null, null, null, null, null)).contains("presupuesto"));
    assertTrue(errorDe(datos("99999999999", null, null, null, null, null)).contains("presupuesto"));
    assertTrue(errorDe(datos(null, "tarde", null, null, null, null)).contains("horario"));
    assertTrue(errorDe(datos(null, null, "6", null, null, null)).contains("limpieza"));
    assertTrue(errorDe(datos(null, null, null, "0", null, null)).contains("ruido"));
    assertTrue(errorDe(datos(null, null, null, null, "siempre", null)).contains("visitas"));
    assertTrue(errorDe(datos(null, null, null, null, null, "x".repeat(1001))).contains("descripción"));
  }

  @Test
  void reconoceLasImagenesPorSuContenido() {
    byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0};
    byte[] jpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
    byte[] gif = "GIF89a".getBytes();
    byte[] webp = "RIFF0000WEBP".getBytes();

    assertDoesNotThrow(() -> ReglasPerfil.validarFoto(new FotoDto("image/png", png)));
    assertDoesNotThrow(() -> ReglasPerfil.validarFoto(new FotoDto("image/jpeg", jpeg)));
    assertDoesNotThrow(() -> ReglasPerfil.validarFoto(new FotoDto("image/gif", gif)));
    assertDoesNotThrow(() -> ReglasPerfil.validarFoto(new FotoDto("image/webp", webp)));

    assertEquals("El archivo seleccionado no es una imagen válida.",
        assertThrows(ErrorNegocio.class, () -> ReglasPerfil.validarFoto(new FotoDto("image/png", jpeg))).getMessage());
    assertEquals("Elige una imagen JPG, PNG, WEBP o GIF.",
        assertThrows(ErrorNegocio.class, () -> ReglasPerfil.validarFoto(new FotoDto("application/pdf", png))).getMessage());
  }

  @Test
  void validaLasUrlDeFoto() {
    assertTrue(ReglasPerfil.esUrlImagenValida("https://ejemplo.com/foto.png"));
    assertTrue(ReglasPerfil.esUrlImagenValida("/uploads/foto-1.png"));
    assertFalse(ReglasPerfil.esUrlImagenValida("ftp://ejemplo.com/foto.png"));
    assertFalse(ReglasPerfil.esUrlImagenValida("javascript:alert(1)"));
    assertFalse(ReglasPerfil.esUrlImagenValida("/uploads/../secreto"));
  }

  @Test
  void conoceLosBarriosDePasto() {
    Barrios barrios = new Barrios();
    assertEquals(338, barrios.total());
    assertTrue(barrios.esValido("El Pilar"));
    assertTrue(barrios.esValido("Otro barrio de Pasto"));
    assertFalse(barrios.esValido("Centro, Pasto"));
    assertFalse(barrios.esValido(""));
    assertFalse(barrios.esValido(null));
  }
}
