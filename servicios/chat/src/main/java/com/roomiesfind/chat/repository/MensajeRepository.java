package com.roomiesfind.chat.repository;

import com.roomiesfind.chat.model.Mensaje;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla mensajes. */
@Repository
public class MensajeRepository {

  private static final RowMapper<Mensaje> FILA = (fila, numero) -> {
    Timestamp leidoEn = fila.getTimestamp("leido_en");
    return new Mensaje(
        fila.getLong("id"), fila.getLong("conversacion_id"), fila.getInt("remitente_id"), fila.getString("contenido"),
        fila.getTimestamp("creado_en").toInstant(), leidoEn == null ? null : leidoEn.toInstant());
  };

  private final JdbcTemplate jdbc;

  public MensajeRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Mensaje crear(long conversacionId, int remitenteId, String contenido) {
    return jdbc.queryForObject(
        "INSERT INTO mensajes (conversacion_id, remitente_id, contenido) VALUES (?, ?, ?) RETURNING *",
        FILA, conversacionId, remitenteId, contenido);
  }

  /** Del más reciente al más antiguo. */
  public List<Mensaje> listar(long conversacionId, Long antesDe, int limite) {
    if (antesDe == null) {
      return jdbc.query("SELECT * FROM mensajes WHERE conversacion_id = ? ORDER BY id DESC LIMIT ?", FILA, conversacionId, limite);
    }
    return jdbc.query(
        "SELECT * FROM mensajes WHERE conversacion_id = ? AND id < ? ORDER BY id DESC LIMIT ?",
        FILA, conversacionId, antesDe, limite);
  }

  public int marcarLeidos(long conversacionId, int remitenteId) {
    return jdbc.update(
        "UPDATE mensajes SET leido_en = now() WHERE conversacion_id = ? AND remitente_id = ? AND leido_en IS NULL",
        conversacionId, remitenteId);
  }
}
