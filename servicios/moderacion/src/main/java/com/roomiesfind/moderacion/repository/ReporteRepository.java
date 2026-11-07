package com.roomiesfind.moderacion.repository;

import com.roomiesfind.moderacion.dto.NuevoReporteDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla reportes. */
@Repository
public class ReporteRepository {

  private final JdbcTemplate jdbc;

  public ReporteRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** @return false si esta persona ya había reportado a esa cuenta */
  public boolean crear(NuevoReporteDto reporte) {
    return jdbc.update(
        """
        INSERT INTO reportes (reportante_id, reportado_id, motivo, detalle)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (reportante_id, reportado_id) DO NOTHING
        """,
        reporte.reportanteId(), reporte.reportadoId(), reporte.motivo(), reporte.detalle()) > 0;
  }

  public int contarRecibidos(int reportadoId) {
    return jdbc.queryForObject("SELECT count(*) FROM reportes WHERE reportado_id = ?", Integer.class, reportadoId);
  }

  public int contarDelUltimoDia(int reportanteId) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM reportes WHERE reportante_id = ? AND creado_en > now() - interval '1 day'",
        Integer.class, reportanteId);
  }

  /** Se libera sola al terminar la transacción. */
  public void esperarTurno(int reportadoId) {
    jdbc.queryForObject("SELECT pg_advisory_xact_lock(?)::text", String.class, (long) reportadoId);
  }
}
