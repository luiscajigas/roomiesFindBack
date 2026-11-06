package com.roomiesfind.compatibilidad.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla traducciones. */
@Repository
public class TraduccionRepository {

  private final JdbcTemplate jdbc;

  public TraduccionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<String> buscar(String huella, String idioma) {
    return jdbc.query(
        "SELECT traduccion FROM traducciones WHERE huella = ? AND idioma = ?",
        (fila, numero) -> fila.getString("traduccion"),
        huella, idioma).stream().findFirst();
  }

  public void guardar(String huella, String idioma, String traduccion) {
    jdbc.update(
        """
        INSERT INTO traducciones (huella, idioma, traduccion) VALUES (?, ?, ?)
        ON CONFLICT (huella, idioma) DO NOTHING
        """,
        huella, idioma, traduccion);
  }
}
