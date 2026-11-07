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
