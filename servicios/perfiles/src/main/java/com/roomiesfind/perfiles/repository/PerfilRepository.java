package com.roomiesfind.perfiles.repository;

import com.roomiesfind.perfiles.model.Perfil;
import com.roomiesfind.perfiles.dto.FiltrosCandidatosDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

  public record PerfilPuntuado(Perfil perfil, int puntaje) {}

  public record Conteos(int total, int muyBien, int buena, int conversar, int distintos) {}

  /**
   * Mismas reglas que calcula el navegador en compatibilidad.worker.ts. Viven también aquí
   * porque para ordenar y paginar hay que conocer el puntaje antes de traer a las personas.
   */
  private static final String PUNTAJE = """
      (CASE WHEN abs(yo.presupuesto - p.presupuesto) <= 50000 THEN 15
            WHEN abs(yo.presupuesto - p.presupuesto) <= 150000 THEN 8 ELSE 0 END
       + CASE WHEN yo.zona = 'Otro barrio de Pasto' OR z.nombre = 'Otro barrio de Pasto' THEN 0
              WHEN lower(trim(yo.zona)) = lower(trim(z.nombre)) THEN 20 ELSE 0 END
       + CASE WHEN yo.horario = p.horario THEN 15
              WHEN yo.horario = 'mixto' OR p.horario = 'mixto' THEN 8 ELSE 0 END
       + CASE WHEN abs(yo.limpieza - p.limpieza) <= 1 THEN 15
              WHEN abs(yo.limpieza - p.limpieza) <= 2 THEN 7 ELSE 0 END
       + CASE WHEN abs(yo.tolerancia_ruido - p.tolerancia_ruido) <= 1 THEN 15
              WHEN abs(yo.tolerancia_ruido - p.tolerancia_ruido) <= 2 THEN 7 ELSE 0 END
       + CASE abs(array_position(ARRAY['nunca','ocasional','frecuente']::varchar[], yo.frecuencia_visitas)
                  - array_position(ARRAY['nunca','ocasional','frecuente']::varchar[], p.frecuencia_visitas))
              WHEN 0 THEN 10 WHEN 1 THEN 5 ELSE 0 END
       + CASE WHEN (yo.tiene_mascotas AND NOT p.acepta_mascotas) OR (p.tiene_mascotas AND NOT yo.acepta_mascotas)
              THEN 0 ELSE 10 END)
      """;

  /** El orden nunca sale de lo que escribe el usuario: ReglasBusqueda lo valida contra esta lista. */
  private static final Map<String, String> ORDENES = Map.of(
      "compatibilidad", "puntaje DESC, usuario_id",
      "presupuesto", "abs(presupuesto - mi_presupuesto), puntaje DESC, usuario_id",
      "recientes", "actualizado_en DESC, usuario_id");

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
    return jdbc.query(SELECT_PERFIL + "WHERE p.usuario_id IN (" + marcadores(usuarioIds.size()) + ")", FILA, usuarioIds.toArray());
  }

  public List<PerfilPuntuado> buscarCandidatos(FiltrosCandidatosDto filtros) {
    List<Object> parametros = new ArrayList<>();
    String base = consultaPuntuada(filtros, parametros);
    parametros.add(filtros.tamano());
    parametros.add((long) (filtros.pagina() - 1) * filtros.tamano());
    return jdbc.query(
        base + " ORDER BY " + ORDENES.get(filtros.orden()) + " LIMIT ? OFFSET ?",
        (fila, numero) -> new PerfilPuntuado(FILA.mapRow(fila, numero), fila.getInt("puntaje")),
        parametros.toArray());
  }

  /** Los límites deben coincidir con los niveles de nivel-compatibilidad.ts. */
  public Conteos contarCandidatos(FiltrosCandidatosDto filtros) {
    List<Object> parametros = new ArrayList<>();
    String base = consultaPuntuada(filtros, parametros);
    return jdbc.queryForObject(
        """
        SELECT count(*) AS total,
               count(*) FILTER (WHERE puntaje >= 85) AS muy_bien,
               count(*) FILTER (WHERE puntaje >= 65 AND puntaje < 85) AS buena,
               count(*) FILTER (WHERE puntaje >= 40 AND puntaje < 65) AS conversar,
               count(*) FILTER (WHERE puntaje < 40) AS distintos
        FROM (%s) AS puntuados
        """.formatted(base),
        (fila, numero) -> new Conteos(fila.getInt("total"), fila.getInt("muy_bien"), fila.getInt("buena"),
            fila.getInt("conversar"), fila.getInt("distintos")),
        parametros.toArray());
  }

  private static String consultaPuntuada(FiltrosCandidatosDto filtros, List<Object> parametros) {
    parametros.add(filtros.usuarioId());
    StringBuilder condiciones = new StringBuilder("p.usuario_id <> yo.usuario_id");
    if (!filtros.excluirIds().isEmpty()) {
      condiciones.append(" AND p.usuario_id NOT IN (").append(marcadores(filtros.excluirIds().size())).append(")");
      parametros.addAll(filtros.excluirIds());
    }
    if (!filtros.zonas().isEmpty()) {
      condiciones.append(" AND z.nombre IN (").append(marcadores(filtros.zonas().size())).append(")");
      parametros.addAll(filtros.zonas());
    }
    if (filtros.horario() != null) {
      condiciones.append(" AND p.horario = ?");
      parametros.add(filtros.horario());
    }
    if (filtros.presupuestoMaximo() != null) {
      condiciones.append(" AND p.presupuesto <= ?");
      parametros.add(filtros.presupuestoMaximo());
    }
    if (filtros.mascotas() != null) {
      condiciones.append(switch (filtros.mascotas()) {
        case "sin_mascotas" -> " AND NOT p.tiene_mascotas";
        case "con_mascotas" -> " AND p.tiene_mascotas";
        default -> " AND p.acepta_mascotas";
      });
    }
    String puntajeMinimo = "";
    if (filtros.puntajeMinimo() != null) {
      puntajeMinimo = " WHERE puntaje >= ?";
      parametros.add(filtros.puntajeMinimo());
    }
    return """
        SELECT * FROM (
          WITH yo AS (
            SELECT mio.*, zona.nombre AS zona FROM perfiles mio JOIN zonas zona ON zona.id = mio.zona_id WHERE mio.usuario_id = ?
          )
          SELECT p.*, z.nombre AS zona, yo.presupuesto AS mi_presupuesto, %s AS puntaje
          FROM perfiles p JOIN zonas z ON z.id = p.zona_id CROSS JOIN yo
          WHERE %s
        ) AS candidatos%s
        """.formatted(PUNTAJE, condiciones, puntajeMinimo);
  }

  private static String marcadores(int cantidad) {
    return String.join(", ", Collections.nCopies(cantidad, "?"));
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
