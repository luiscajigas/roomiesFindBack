package com.roomiesfind.perfiles.repository;

import com.roomiesfind.perfiles.model.Perfil;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla perfiles. */
@Repository
public class PerfilRepository {

  public record NuevoPerfil(int usuarioId, int presupuesto, int zonaId, String horario, int limpieza,
      int toleranciaRuido, String frecuenciaVisitas, boolean tieneMascotas, boolean aceptaMascotas,
      String descripcion, String fotoUrl) {}

  /** Los campos en null no se modifican. */
  public record CambiosPerfil(Integer presupuesto, Integer zonaId, String horario, Integer limpieza,
      Integer toleranciaRuido, String frecuenciaVisitas, Boolean tieneMascotas, Boolean aceptaMascotas,
      String descripcion, String fotoUrl) {}

  private static final String SELECT_PERFIL =
      "SELECT p.*, z.nombre AS zona FROM perfiles p JOIN zonas z ON z.id = p.zona_id ";

  private static final RowMapper<Perfil> FILA = (fila, numero) -> new Perfil(
      fila.getInt("usuario_id"), fila.getInt("presupuesto"), fila.getInt("zona_id"), fila.getString("zona"),
      fila.getString("horario"), fila.getInt("limpieza"), fila.getInt("tolerancia_ruido"),
      fila.getString("frecuencia_visitas"), fila.getBoolean("tiene_mascotas"), fila.getBoolean("acepta_mascotas"),
      fila.getString("descripcion"), fila.getString("foto_url"),
      fila.getTimestamp("actualizado_en").toLocalDateTime());

  private final JdbcTemplate jdbc;

  public PerfilRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void crear(NuevoPerfil perfil) {
    jdbc.update(
        """
        INSERT INTO perfiles
          (usuario_id, presupuesto, zona_id, horario, limpieza, tolerancia_ruido, frecuencia_visitas,
           tiene_mascotas, acepta_mascotas, descripcion, foto_url)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        perfil.usuarioId(), perfil.presupuesto(), perfil.zonaId(), perfil.horario(), perfil.limpieza(),
        perfil.toleranciaRuido(), perfil.frecuenciaVisitas(), perfil.tieneMascotas(), perfil.aceptaMascotas(),
        perfil.descripcion(), perfil.fotoUrl());
  }

  public Optional<Perfil> buscar(int usuarioId) {
    return jdbc.query(SELECT_PERFIL + "WHERE p.usuario_id = ?", FILA, usuarioId).stream().findFirst();
  }

  public List<Perfil> buscarVarios(List<Integer> usuarioIds) {
    if (usuarioIds.isEmpty()) return List.of();
    String marcadores = String.join(", ", Collections.nCopies(usuarioIds.size(), "?"));
    return jdbc.query(SELECT_PERFIL + "WHERE p.usuario_id IN (" + marcadores + ")", FILA, usuarioIds.toArray());
  }

  public List<Perfil> listarExcepto(int usuarioId) {
    return jdbc.query(SELECT_PERFIL + "WHERE p.usuario_id != ? ORDER BY p.usuario_id", FILA, usuarioId);
  }

  public void actualizar(int usuarioId, CambiosPerfil cambios) {
    jdbc.update(
        """
        UPDATE perfiles SET
          presupuesto = COALESCE(?::integer, presupuesto),
          zona_id = COALESCE(?::integer, zona_id),
          horario = COALESCE(?::varchar, horario),
          limpieza = COALESCE(?::smallint, limpieza),
          tolerancia_ruido = COALESCE(?::smallint, tolerancia_ruido),
          frecuencia_visitas = COALESCE(?::varchar, frecuencia_visitas),
          tiene_mascotas = COALESCE(?::boolean, tiene_mascotas),
          acepta_mascotas = COALESCE(?::boolean, acepta_mascotas),
          descripcion = COALESCE(?::text, descripcion),
          foto_url = COALESCE(?::text, foto_url),
          actualizado_en = now()
        WHERE usuario_id = ?
        """,
        cambios.presupuesto(), cambios.zonaId(), cambios.horario(), cambios.limpieza(), cambios.toleranciaRuido(),
        cambios.frecuenciaVisitas(), cambios.tieneMascotas(), cambios.aceptaMascotas(), cambios.descripcion(),
        cambios.fotoUrl(), usuarioId);
  }

  public boolean actualizarRutaFoto(int usuarioId, String ruta) {
    return jdbc.update("UPDATE perfiles SET foto_url = ?, actualizado_en = now() WHERE usuario_id = ?", ruta, usuarioId) > 0;
  }
}
