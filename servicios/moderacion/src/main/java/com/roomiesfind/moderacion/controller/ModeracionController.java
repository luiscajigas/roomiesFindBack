package com.roomiesfind.moderacion.controller;

import com.roomiesfind.moderacion.dto.CuentaReportadaDto;
import com.roomiesfind.moderacion.dto.NuevoReporteDto;
import com.roomiesfind.moderacion.dto.ResumenDto;
import com.roomiesfind.moderacion.model.Suspension;
import com.roomiesfind.moderacion.service.ModeracionService;
import java.util.List;
import org.springframework.stereotype.Component;

/** Valida la entrada y coordina los casos de uso. No conoce gRPC ni SQL. */
@Component
public class ModeracionController {

  private final ModeracionService moderacion;

  public ModeracionController(ModeracionService moderacion) {
    this.moderacion = moderacion;
  }

  public void bloquear(int usuarioId, int otroId) {
    ReglasModeracion.validarPareja(usuarioId, otroId, "No puedes bloquear tu propia cuenta.");
    moderacion.bloquear(usuarioId, otroId);
  }

  public void desbloquear(int usuarioId, int otroId) {
    ReglasModeracion.validarPareja(usuarioId, otroId, "No puedes bloquear tu propia cuenta.");
    moderacion.desbloquear(usuarioId, otroId);
  }

  public List<Integer> bloqueados(int usuarioId) {
    return moderacion.bloqueados(usuarioId);
  }

  public List<Integer> excluidos(int usuarioId) {
    return moderacion.excluidos(usuarioId);
  }

  public boolean estanBloqueados(int usuarioId, int otroId) {
    return moderacion.estanBloqueados(usuarioId, otroId);
  }

  /** @return true si este reporte hizo que la cuenta recibiera la alerta */
  public boolean reportar(NuevoReporteDto reporte) {
    return moderacion.reportar(ReglasModeracion.validarReporte(reporte));
  }

  public List<Integer> cuentasConAlerta(List<Integer> ids) {
    return moderacion.cuentasConAlerta(ids);
  }

  public ResumenDto resumen() {
    return moderacion.resumen();
  }

  public List<CuentaReportadaDto> cuentasReportadas(boolean soloPendientes) {
    return moderacion.cuentasReportadas(soloPendientes);
  }

  public void resolver(int adminId, int usuarioId, String decision) {
    ReglasModeracion.validarPareja(adminId, usuarioId, "No puedes revisar reportes sobre tu propia cuenta.");
    ReglasModeracion.validarDecision(decision);
    moderacion.resolver(adminId, usuarioId, decision);
  }

  public void suspender(int adminId, int usuarioId, String motivo) {
    moderacion.suspender(adminId, usuarioId, ReglasModeracion.validarMotivoSuspension(adminId, usuarioId, motivo));
  }

  public void reactivar(int usuarioId) {
    moderacion.reactivar(usuarioId);
  }

  public List<Suspension> suspendidas() {
    return moderacion.suspendidas();
  }

  public boolean estaSuspendida(int usuarioId) {
    return moderacion.estaSuspendida(usuarioId);
  }
}
