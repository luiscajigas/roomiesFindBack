package com.roomiesfind.usuarios.repository;

import com.roomiesfind.usuarios.model.Usuario;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Único acceso a la tabla usuarios. */
@Repository
public class UsuarioRepository {

  private static final RowMapper<Usuario> FILA = (fila, numero) -> new Usuario(
      fila.getInt("id"), fila.getString("nombre"), fila.getString("email"), fila.getString("password_hash"), fila.getString("rol"));

  private final NamedParameterJdbcTemplate jdbc;

  public UsuarioRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Usuario crear(String nombre, String email, String passwordHash) {
    return jdbc.queryForObject(
        "INSERT INTO usuarios (nombre, email, password_hash) VALUES (:nombre, :email, :hash) RETURNING *",
        new MapSqlParameterSource().addValue("nombre", nombre).addValue("email", email).addValue("hash", passwordHash),
        FILA);
  }

  public Optional<Usuario> buscarPorEmail(String email) {
    return jdbc.query("SELECT * FROM usuarios WHERE email = :email", new MapSqlParameterSource("email", email), FILA)
        .stream().findFirst();
  }

  public List<Usuario> buscarPorIds(List<Integer> ids) {
    if (ids.isEmpty()) return Collections.emptyList();
    return jdbc.query("SELECT * FROM usuarios WHERE id IN (:ids)", new MapSqlParameterSource("ids", ids), FILA);
  }

  public void eliminar(int id) {
    jdbc.update("DELETE FROM usuarios WHERE id = :id", new MapSqlParameterSource("id", id));
  }
}
