package com.roomiesfind.perfiles.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla zonas. */
@Repository
public class ZonaRepository {

  private final JdbcTemplate jdbc;

  public ZonaRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public int obtenerOCrear(String nombre) {
    return jdbc.queryForObject(
        """
        INSERT INTO zonas (nombre) VALUES (?)
        ON CONFLICT (nombre) DO UPDATE SET nombre = EXCLUDED.nombre
        RETURNING id
        """,
        Integer.class, nombre);
  }
}
