package com.roomiesfind.chat.repository;

import com.roomiesfind.chat.dto.ConversacionDto;
import com.roomiesfind.chat.dto.MensajeDto;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla conversaciones. Cada pareja se guarda con el id menor primero, para que sea única. */
@Repository
public class ConversacionRepository {

  private final JdbcTemplate jdbc;

  public ConversacionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<Long> buscar(int usuarioId, int otroId) {
    return jdbc.queryForList(
        "SELECT id FROM conversaciones WHERE usuario_a = ? AND usuario_b = ?",
        Long.class, Math.min(usuarioId, otroId), Math.max(usuarioId, otroId)).stream().findFirst();
  }

  public long obtenerOCrear(int usuarioId, int otroId) {
    return jdbc.queryForObject(
        """
        INSERT INTO conversaciones (usuario_a, usuario_b) VALUES (?, ?)
        ON CONFLICT (usuario_a, usuario_b) DO UPDATE SET usuario_a = EXCLUDED.usuario_a
        RETURNING id
        """,
        Long.class, Math.min(usuarioId, otroId), Math.max(usuarioId, otroId));
  }

  /** Conversaciones de una persona, con su último mensaje y lo que le falta por leer. */
  public List<ConversacionDto> listar(int usuarioId) {
    return jdbc.query(
        """
        SELECT CASE WHEN c.usuario_a = ? THEN c.usuario_b ELSE c.usuario_a END AS otro_id,
               m.id, m.remitente_id, m.contenido, m.creado_en, m.leido_en,
               (SELECT count(*) FROM mensajes x
                 WHERE x.conversacion_id = c.id AND x.remitente_id <> ? AND x.leido_en IS NULL) AS no_leidos
        FROM conversaciones c
        JOIN LATERAL (
          SELECT * FROM mensajes WHERE conversacion_id = c.id ORDER BY id DESC LIMIT 1
        ) m ON true
        WHERE c.usuario_a = ? OR c.usuario_b = ?
        ORDER BY m.id DESC
        """,
        (fila, numero) -> {
          int otroId = fila.getInt("otro_id");
          int remitenteId = fila.getInt("remitente_id");
          var leidoEn = fila.getTimestamp("leido_en");
          MensajeDto ultimo = new MensajeDto(
              fila.getLong("id"), remitenteId, remitenteId == usuarioId ? otroId : usuarioId,
              fila.getString("contenido"), fila.getTimestamp("creado_en").toInstant().toString(),
              leidoEn == null ? null : leidoEn.toInstant().toString());
          return new ConversacionDto(otroId, ultimo, fila.getInt("no_leidos"));
        },
        usuarioId, usuarioId, usuarioId, usuarioId);
  }
}
