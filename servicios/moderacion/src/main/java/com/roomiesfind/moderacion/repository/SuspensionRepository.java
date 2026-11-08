package com.roomiesfind.moderacion.repository;

import com.roomiesfind.moderacion.model.Suspension;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla suspensiones. */
@Repository
public class SuspensionRepository {

  private static final RowMapper<Suspension> FILA = (fila, numero) -> new Suspension(
      fila.getInt("usuario_id"), fila.getString("motivo"), (Integer) fila.getObject("suspendida_por"),
      fila.getTimestamp("creada_en").toInstant().toString());

  private final JdbcTemplate jdbc;

  public SuspensionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void suspender(int usuarioId, String motivo, int adminId) {
    jdbc.update(
        """
        INSERT INTO suspensiones (usuario_id, motivo, suspendida_por) VALUES (?, ?, ?)
        ON CONFLICT (usuario_id) DO UPDATE SET motivo = EXCLUDED.motivo, suspendida_por = EXCLUDED.suspendida_por
        """,
        usuarioId, motivo, adminId);
  }

  public boolean reactivar(int usuarioId) {
    return jdbc.update("DELETE FROM suspensiones WHERE usuario_id = ?", usuarioId) > 0;
  }

  public boolean existe(int usuarioId) {
    return Boolean.TRUE.equals(jdbc.queryForObject(
        "SELECT EXISTS (SELECT 1 FROM suspensiones WHERE usuario_id = ?)", Boolean.class, usuarioId));
  }

  public List<Suspension> listar() {
    return jdbc.query("SELECT * FROM suspensiones ORDER BY creada_en DESC", FILA);
  }

  public int contar() {
    return jdbc.queryForObject("SELECT count(*) FROM suspensiones", Integer.class);
  }
}
