package com.roomiesfind.moderacion.repository;

import com.roomiesfind.moderacion.dto.NuevoReporteDto;
import com.roomiesfind.moderacion.model.Reporte;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla reportes. */
@Repository
public class ReporteRepository {

  private static final int CUENTAS_MAXIMAS = 100;

  private static final RowMapper<Reporte> FILA = (fila, numero) -> new Reporte(
      fila.getLong("id"), fila.getInt("reportante_id"), fila.getInt("reportado_id"), fila.getString("motivo"),
      fila.getString("detalle"), fila.getString("estado"), fila.getTimestamp("creado_en").toInstant().toString());

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

  /** Los reportes descartados por un administrador ya no cuentan para la alerta. */
  public int contarRecibidos(int reportadoId) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM reportes WHERE reportado_id = ? AND estado <> 'descartado'", Integer.class, reportadoId);
  }

  /** Reportes de las cuentas más recientes primero; con soloPendientes, solo cuentas con algo por revisar. */
  public List<Reporte> listar(boolean soloPendientes) {
    return jdbc.query(
        """
        SELECT r.* FROM reportes r
        JOIN (
          SELECT reportado_id, max(creado_en) AS ultimo FROM reportes
          WHERE NOT ? OR reportado_id IN (SELECT reportado_id FROM reportes WHERE estado = 'pendiente')
          GROUP BY reportado_id ORDER BY ultimo DESC LIMIT ?
        ) cuentas ON cuentas.reportado_id = r.reportado_id
        ORDER BY cuentas.ultimo DESC, r.reportado_id, r.creado_en DESC
        """,
        FILA, soloPendientes, CUENTAS_MAXIMAS);
  }

  /** @param desde un estado, o "cualquiera" para todos los que no estén ya en el nuevo estado
   * @return cuántos reportes cambiaron */
  public int marcar(int reportadoId, String desde, String estado, int adminId) {
    return jdbc.update(
        """
        UPDATE reportes SET estado = ?, revisado_en = now(), revisado_por = ?
        WHERE reportado_id = ? AND (estado = ? OR (? = 'cualquiera' AND estado <> ?))
        """,
        estado, adminId, reportadoId, desde, desde, estado);
  }

  public int contarPendientes() {
    return jdbc.queryForObject("SELECT count(*) FROM reportes WHERE estado = 'pendiente'", Integer.class);
  }

  public int contarCuentasPendientes() {
    return jdbc.queryForObject("SELECT count(DISTINCT reportado_id) FROM reportes WHERE estado = 'pendiente'", Integer.class);
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
