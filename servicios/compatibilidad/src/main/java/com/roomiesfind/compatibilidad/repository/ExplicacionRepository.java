package com.roomiesfind.compatibilidad.repository;

import com.roomiesfind.compatibilidad.model.Explicacion;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla explicaciones_ia. */
@Repository
public class ExplicacionRepository {

  private final JdbcTemplate jdbc;

  public ExplicacionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<Explicacion> buscarGenerada(int usuarioId, int candidatoId, int score) {
    return jdbc.query(
        """
        SELECT id, explicacion FROM explicaciones_ia
        WHERE usuario_id = ? AND candidato_id = ? AND score = ?
          AND estado = 'generado' AND explicacion IS NOT NULL
        ORDER BY creado_en DESC LIMIT 1
        """,
        (fila, numero) -> new Explicacion(fila.getInt("id"), fila.getString("explicacion")),
        usuarioId, candidatoId, score).stream().findFirst();
  }

  public int crearPendiente(int usuarioId, int candidatoId, int score) {
    return jdbc.queryForObject(
        """
        INSERT INTO explicaciones_ia (usuario_id, candidato_id, score, explicacion, estado)
        VALUES (?, ?, ?, NULL, 'pendiente') RETURNING id
        """,
        Integer.class, usuarioId, candidatoId, score);
  }

  public void marcarGenerada(int id, String texto) {
    jdbc.update("UPDATE explicaciones_ia SET explicacion = ?, estado = 'generado' WHERE id = ?", texto, id);
  }
}
