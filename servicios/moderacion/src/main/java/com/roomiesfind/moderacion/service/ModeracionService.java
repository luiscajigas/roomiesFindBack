package com.roomiesfind.moderacion.service;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.moderacion.dto.NuevoReporteDto;
import com.roomiesfind.moderacion.repository.AlertaRepository;
import com.roomiesfind.moderacion.repository.BloqueoRepository;
import com.roomiesfind.moderacion.repository.ReporteRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de bloqueos y reportes. Una cuenta recibe alerta cuando la reportan personas
 * distintas: cada una cuenta una sola vez, para que nadie pueda provocarla insistiendo.
 */
@Service
public class ModeracionService {

  public static final int REPORTES_PARA_ALERTA = 3;
  private static final int REPORTES_POR_DIA = 10;
  private static final String PERSONA_NO_EXISTE = "La persona no existe.";

  private final BloqueoRepository bloqueos;
  private final ReporteRepository reportes;
  private final AlertaRepository alertas;

  public ModeracionService(BloqueoRepository bloqueos, ReporteRepository reportes, AlertaRepository alertas) {
    this.bloqueos = bloqueos;
    this.reportes = reportes;
    this.alertas = alertas;
  }

  public void bloquear(int usuarioId, int otroId) {
    try {
      bloqueos.crear(usuarioId, otroId);
    } catch (DataIntegrityViolationException error) {
      throw ErrorNegocio.noEncontrado(PERSONA_NO_EXISTE);
    }
  }

  public void desbloquear(int usuarioId, int otroId) {
    bloqueos.eliminar(usuarioId, otroId);
  }

  public List<Integer> bloqueados(int usuarioId) {
    return bloqueos.listarBloqueados(usuarioId);
  }

  public List<Integer> excluidos(int usuarioId) {
    return bloqueos.listarExcluidos(usuarioId);
  }

  public boolean estanBloqueados(int usuarioId, int otroId) {
    return bloqueos.existeEntre(usuarioId, otroId);
  }

  @Transactional
  public boolean reportar(NuevoReporteDto reporte) {
    if (reportes.contarDelUltimoDia(reporte.reportanteId()) >= REPORTES_POR_DIA) {
      throw ErrorNegocio.conflicto("Has enviado muchos reportes hoy. Inténtalo mañana.");
    }
    // Dos reportes simultáneos no deben contar a la vez y dejar la cuenta sin su alerta.
    reportes.esperarTurno(reporte.reportadoId());
    try {
      if (!reportes.crear(reporte)) throw ErrorNegocio.conflicto("Ya reportaste a esta persona.");
    } catch (DataIntegrityViolationException error) {
      throw ErrorNegocio.noEncontrado(PERSONA_NO_EXISTE);
    }
    return reportes.contarRecibidos(reporte.reportadoId()) >= REPORTES_PARA_ALERTA
        && alertas.activar(reporte.reportadoId());
  }

  public List<Integer> cuentasConAlerta(List<Integer> ids) {
    return ids.isEmpty() ? List.of() : alertas.deEntre(ids);
  }
}
