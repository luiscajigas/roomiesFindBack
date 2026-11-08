package com.roomiesfind.moderacion.service;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.moderacion.dto.CuentaReportadaDto;
import com.roomiesfind.moderacion.dto.NuevoReporteDto;
import com.roomiesfind.moderacion.dto.ResumenDto;
import com.roomiesfind.moderacion.model.Reporte;
import com.roomiesfind.moderacion.model.Suspension;
import com.roomiesfind.moderacion.repository.AlertaRepository;
import com.roomiesfind.moderacion.repository.BloqueoRepository;
import com.roomiesfind.moderacion.repository.ReporteRepository;
import com.roomiesfind.moderacion.repository.SuspensionRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private final SuspensionRepository suspensiones;

  public ModeracionService(BloqueoRepository bloqueos, ReporteRepository reportes, AlertaRepository alertas,
      SuspensionRepository suspensiones) {
    this.bloqueos = bloqueos;
    this.reportes = reportes;
    this.alertas = alertas;
    this.suspensiones = suspensiones;
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

  public ResumenDto resumen() {
    return new ResumenDto(reportes.contarCuentasPendientes(), reportes.contarPendientes(), alertas.contar(), suspensiones.contar());
  }

  public List<CuentaReportadaDto> cuentasReportadas(boolean soloPendientes) {
    Map<Integer, List<Reporte>> porCuenta = new LinkedHashMap<>();
    for (Reporte reporte : reportes.listar(soloPendientes)) {
      porCuenta.computeIfAbsent(reporte.reportadoId(), (id) -> new ArrayList<>()).add(reporte);
    }
    List<Integer> ids = List.copyOf(porCuenta.keySet());
    Set<Integer> conAlerta = new HashSet<>(cuentasConAlerta(ids));
    Set<Integer> suspendidas = new HashSet<>();
    for (Suspension suspension : suspensiones.listar()) suspendidas.add(suspension.usuarioId());
    return porCuenta.entrySet().stream()
        .map((cuenta) -> new CuentaReportadaDto(cuenta.getKey(), cuenta.getValue(),
            conAlerta.contains(cuenta.getKey()), suspendidas.contains(cuenta.getKey())))
        .toList();
  }

  /** Descartar quita la alerta y los reportes dejan de contar; mantener solo los marca como revisados. */
  @Transactional
  public void resolver(int adminId, int usuarioId, String decision) {
    reportes.esperarTurno(usuarioId);
    if ("descartar".equals(decision)) {
      if (reportes.marcar(usuarioId, "cualquiera", "descartado", adminId) == 0) throw sinReportes();
      alertas.quitar(usuarioId);
    } else if (reportes.marcar(usuarioId, "pendiente", "revisado", adminId) == 0) {
      throw sinReportes();
    }
  }

  @Transactional
  public void suspender(int adminId, int usuarioId, String motivo) {
    try {
      suspensiones.suspender(usuarioId, motivo, adminId);
    } catch (DataIntegrityViolationException error) {
      throw ErrorNegocio.noEncontrado(PERSONA_NO_EXISTE);
    }
    reportes.marcar(usuarioId, "pendiente", "revisado", adminId);
  }

  public void reactivar(int usuarioId) {
    if (!suspensiones.reactivar(usuarioId)) throw ErrorNegocio.noEncontrado("Esta cuenta no está suspendida.");
  }

  public List<Suspension> suspendidas() {
    return suspensiones.listar();
  }

  public boolean estaSuspendida(int usuarioId) {
    return suspensiones.existe(usuarioId);
  }

  private static ErrorNegocio sinReportes() {
    return ErrorNegocio.noEncontrado("No hay reportes para revisar.");
  }
}
