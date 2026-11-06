package com.roomiesfind.compatibilidad.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.compatibilidad.dto.SolicitudExplicacionDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompatibilidadControllerTest {

  private final CompatibilidadController controller = new CompatibilidadController(null);

  @Test
  void validaIdentificadoresYPuntajes() {
    assertTrue(CompatibilidadController.esIdValido(5));
    assertFalse(CompatibilidadController.esIdValido(0));
    assertFalse(CompatibilidadController.esIdValido(-1));
    assertFalse(CompatibilidadController.esIdValido(1.5));
    assertFalse(CompatibilidadController.esIdValido(Double.NaN));

    assertTrue(CompatibilidadController.esPuntajeValido(0));
    assertTrue(CompatibilidadController.esPuntajeValido(100));
    assertFalse(CompatibilidadController.esPuntajeValido(101));
    assertFalse(CompatibilidadController.esPuntajeValido(-1));
    assertFalse(CompatibilidadController.esPuntajeValido(Double.NaN));
  }

  @Test
  void exigeCandidatoYPuntajeAlRegistrar() {
    assertEquals("candidatoId y score son obligatorios.",
        assertThrows(ErrorNegocio.class, () -> controller.registrar(1, null, 50.0)).getMessage());
    assertEquals("candidatoId y score son obligatorios.",
        assertThrows(ErrorNegocio.class, () -> controller.registrar(1, 2.0, null)).getMessage());
    assertEquals("candidatoId o score no son válidos.",
        assertThrows(ErrorNegocio.class, () -> controller.registrar(1, 1.0, 50.0)).getMessage());
  }

  @Test
  void noExplicaLaCompatibilidadConsigoMismo() {
    assertEquals("No puedes pedir una explicación de compatibilidad contigo mismo.",
        assertThrows(ErrorNegocio.class, () -> controller.explicar(
            new SolicitudExplicacionDto(1, 1, 50, List.of(), List.of(), "es"))).getMessage());
  }
}
