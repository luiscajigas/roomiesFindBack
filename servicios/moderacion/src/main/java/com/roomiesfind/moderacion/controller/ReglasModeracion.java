package com.roomiesfind.moderacion.controller;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.moderacion.dto.NuevoReporteDto;
import com.roomiesfind.moderacion.model.MotivoReporte;

/** Reglas de validación de bloqueos y reportes. */
final class ReglasModeracion {

  static final int DETALLE_MAXIMO = 500;

  private ReglasModeracion() {}

  static void validarPareja(int usuarioId, int otroId, String mensajeMismaCuenta) {
    if (otroId <= 0) throw ErrorNegocio.solicitudInvalida("La persona no existe.");
    if (usuarioId == otroId) throw ErrorNegocio.solicitudInvalida(mensajeMismaCuenta);
  }

  static String validarMotivoSuspension(int adminId, int usuarioId, String motivo) {
    validarPareja(adminId, usuarioId, "No puedes suspender tu propia cuenta.");
    String texto = motivo == null ? "" : motivo.strip();
    if (texto.isEmpty()) throw ErrorNegocio.solicitudInvalida("Escribe el motivo de la suspensión.");
    if (texto.length() > DETALLE_MAXIMO) throw ErrorNegocio.solicitudInvalida("El motivo puede tener máximo 500 caracteres.");
    return texto;
  }

  static void validarDecision(String decision) {
    if (!"descartar".equals(decision) && !"mantener".equals(decision)) {
      throw ErrorNegocio.solicitudInvalida("La decisión no es válida.");
    }
  }

  static NuevoReporteDto validarReporte(NuevoReporteDto reporte) {
    validarPareja(reporte.reportanteId(), reporte.reportadoId(), "No puedes reportar tu propia cuenta.");
    if (!MotivoReporte.existe(reporte.motivo())) throw ErrorNegocio.solicitudInvalida("Elige un motivo de la lista.");

    String detalle = reporte.detalle() == null ? "" : reporte.detalle().strip();
    if (detalle.length() > DETALLE_MAXIMO) throw ErrorNegocio.solicitudInvalida("El detalle puede tener máximo 500 caracteres.");
    if (detalle.isEmpty() && MotivoReporte.OTRO.valor().equals(reporte.motivo())) {
      throw ErrorNegocio.solicitudInvalida("Cuéntanos qué pasó.");
    }
    return new NuevoReporteDto(reporte.reportanteId(), reporte.reportadoId(), reporte.motivo(), detalle.isEmpty() ? null : detalle);
  }
}
