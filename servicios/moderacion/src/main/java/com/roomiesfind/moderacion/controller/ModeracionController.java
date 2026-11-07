package com.roomiesfind.moderacion.controller;

import com.roomiesfind.moderacion.dto.NuevoReporteDto;
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
}
