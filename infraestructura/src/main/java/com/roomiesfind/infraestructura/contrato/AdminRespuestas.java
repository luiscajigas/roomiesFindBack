package com.roomiesfind.infraestructura.contrato;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Respuestas del panel de administración. */
public final class AdminRespuestas {

  private AdminRespuestas() {}

  /** Datos de una cuenta para el panel: a diferencia de PersonaRespuesta, incluye correo y rol. */
  public record Ficha(int id, String nombre, String email, String rol, @JsonProperty("foto_url") String fotoUrl) {

    public PersonaCorta corta() {
      return new PersonaCorta(id, nombre);
    }
  }

  public record PersonaCorta(int id, String nombre) {}

  /** @param reportante null si esa cuenta ya fue borrada */
  public record Reporte(long id, String motivo, String detalle, String estado, String creadoEn, PersonaCorta reportante) {}

  public record CuentaReportada(Ficha persona, boolean alerta, boolean suspendida, List<Reporte> reportes) {}

  public record Suspension(Ficha persona, String motivo, String creadaEn, PersonaCorta suspendidaPor) {}

  public record Resumen(int cuentasPorRevisar, int reportesPendientes, int alertasActivas, int cuentasSuspendidas) {}
}
