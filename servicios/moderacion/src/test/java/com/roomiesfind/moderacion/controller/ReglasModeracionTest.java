package com.roomiesfind.moderacion.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.moderacion.dto.NuevoReporteDto;
import org.junit.jupiter.api.Test;

class ReglasModeracionTest {

  private static String errorDe(int reportado, String motivo, String detalle) {
    return assertThrows(ErrorNegocio.class,
        () -> ReglasModeracion.validarReporte(new NuevoReporteDto(1, reportado, motivo, detalle))).getMessage();
  }

  @Test
  void noPermiteActuarSobreLaPropiaCuentaNiSobreIdsInvalidos() {
    assertEquals("No puedes reportar tu propia cuenta.", errorDe(1, "acoso", ""));
    assertEquals("La persona no existe.", errorDe(0, "acoso", ""));
    assertThrows(ErrorNegocio.class, () -> ReglasModeracion.validarPareja(1, 1, "No puedes bloquear tu propia cuenta."));
  }

  @Test
  void soloAceptaMotivosDeLaLista() {
    assertEquals("Elige un motivo de la lista.", errorDe(2, "no me cae bien", ""));
    assertEquals("Elige un motivo de la lista.", errorDe(2, "", ""));
  }

  @Test
  void elMotivoOtroExigeDetalleYElDetalleTieneLimite() {
    assertEquals("Cuéntanos qué pasó.", errorDe(2, "otro", "   "));
    assertEquals("El detalle puede tener máximo 500 caracteres.", errorDe(2, "acoso", "x".repeat(501)));
  }

  @Test
  void laSuspensionExigeMotivoYNoAplicaALaPropiaCuenta() {
    assertEquals("No puedes suspender tu propia cuenta.",
        assertThrows(ErrorNegocio.class, () -> ReglasModeracion.validarMotivoSuspension(1, 1, "x")).getMessage());
    assertEquals("Escribe el motivo de la suspensión.",
        assertThrows(ErrorNegocio.class, () -> ReglasModeracion.validarMotivoSuspension(1, 2, "  ")).getMessage());
    assertThrows(ErrorNegocio.class, () -> ReglasModeracion.validarMotivoSuspension(1, 2, "x".repeat(501)));
    assertEquals("Estafa confirmada", ReglasModeracion.validarMotivoSuspension(1, 2, " Estafa confirmada "));
  }

  @Test
  void soloAceptaDecisionesConocidas() {
    ReglasModeracion.validarDecision("descartar");
    ReglasModeracion.validarDecision("mantener");
    assertThrows(ErrorNegocio.class, () -> ReglasModeracion.validarDecision("borrar"));
    assertThrows(ErrorNegocio.class, () -> ReglasModeracion.validarDecision(null));
  }

  @Test
  void limpiaElDetalle() {
    assertEquals("hola", ReglasModeracion.validarReporte(new NuevoReporteDto(1, 2, "estafa", "  hola ")).detalle());
    assertNull(ReglasModeracion.validarReporte(new NuevoReporteDto(1, 2, "estafa", "  ")).detalle());
  }
}
