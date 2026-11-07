package com.roomiesfind.moderacion.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla bloqueos. */
@Repository
public class BloqueoRepository {

  private final JdbcTemplate jdbc;

  public BloqueoRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void crear(int bloqueadorId, int bloqueadoId) {
    jdbc.update(
        "INSERT INTO bloqueos (bloqueador_id, bloqueado_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
        bloqueadorId, bloqueadoId);
  }

  public void eliminar(int bloqueadorId, int bloqueadoId) {
    jdbc.update("DELETE FROM bloqueos WHERE bloqueador_id = ? AND bloqueado_id = ?", bloqueadorId, bloqueadoId);
  }

  public List<Integer> listarBloqueados(int bloqueadorId) {
    return jdbc.queryForList(
        "SELECT bloqueado_id FROM bloqueos WHERE bloqueador_id = ? ORDER BY creado_en DESC",
        Integer.class, bloqueadorId);
  }

  public List<Integer> listarExcluidos(int usuarioId) {
    return jdbc.queryForList(
        """
        SELECT bloqueado_id FROM bloqueos WHERE bloqueador_id = ?
        UNION
        SELECT bloqueador_id FROM bloqueos WHERE bloqueado_id = ?
        """,
        Integer.class, usuarioId, usuarioId);
  }

  public boolean existeEntre(int usuarioId, int otroId) {
    return Boolean.TRUE.equals(jdbc.queryForObject(
        """
        SELECT EXISTS (
          SELECT 1 FROM bloqueos
          WHERE (bloqueador_id = ? AND bloqueado_id = ?) OR (bloqueador_id = ? AND bloqueado_id = ?)
        )
        """,
        Boolean.class, usuarioId, otroId, otroId, usuarioId));
  }
}
