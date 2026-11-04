package com.roomiesfind.compatibilidad.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla recomendaciones. */
@Repository
public class RecomendacionRepository {

  private final JdbcTemplate jdbc;

  public RecomendacionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void guardar(int usuarioId, int candidatoId, int score) {
    jdbc.update(
        """
        INSERT INTO recomendaciones (usuario_id, candidato_id, score)
        VALUES (?, ?, ?)
        ON CONFLICT (usuario_id, candidato_id)
        DO UPDATE SET score = EXCLUDED.score, actualizado_en = now()
        """,
        usuarioId, candidatoId, score);
  }
}
