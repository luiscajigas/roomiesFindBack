package com.roomiesfind.moderacion.repository;

import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla alertas_cuenta. */
@Repository
public class AlertaRepository {

  private final JdbcTemplate jdbc;

  public AlertaRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** @return true solo la primera vez, para avisar una sola vez */
  public boolean activar(int usuarioId) {
    return jdbc.update("INSERT INTO alertas_cuenta (usuario_id) VALUES (?) ON CONFLICT DO NOTHING", usuarioId) > 0;
  }

  public List<Integer> deEntre(List<Integer> ids) {
    String marcadores = String.join(", ", Collections.nCopies(ids.size(), "?"));
    return jdbc.queryForList(
        "SELECT usuario_id FROM alertas_cuenta WHERE usuario_id IN (" + marcadores + ")",
        Integer.class, ids.toArray());
  }
}
