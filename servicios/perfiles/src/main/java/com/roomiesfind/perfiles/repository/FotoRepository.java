package com.roomiesfind.perfiles.repository;

import com.roomiesfind.perfiles.dto.FotoDto;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla fotos_perfil. */
@Repository
public class FotoRepository {

  private final JdbcTemplate jdbc;

  public FotoRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<FotoDto> buscar(int usuarioId) {
    return jdbc.query(
        "SELECT tipo_mime, contenido FROM fotos_perfil WHERE usuario_id = ?",
        (fila, numero) -> new FotoDto(fila.getString("tipo_mime"), fila.getBytes("contenido")),
        usuarioId).stream().findFirst();
  }

  public void guardar(int usuarioId, FotoDto foto) {
    jdbc.update(
        """
        INSERT INTO fotos_perfil (usuario_id, tipo_mime, contenido, actualizado_en)
        VALUES (?, ?, ?, now())
        ON CONFLICT (usuario_id) DO UPDATE SET
          tipo_mime = EXCLUDED.tipo_mime,
          contenido = EXCLUDED.contenido,
          actualizado_en = now()
        """,
        usuarioId, foto.tipoMime(), foto.contenido());
  }
}
