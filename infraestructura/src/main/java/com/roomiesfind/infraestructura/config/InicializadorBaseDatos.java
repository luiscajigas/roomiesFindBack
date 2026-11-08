package com.roomiesfind.infraestructura.config;

import com.roomiesfind.comun.ConexionBaseDatos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Crea las tablas que falten antes de recibir peticiones. Todos los servicios
 * comparten una sola base; es seguro ejecutarlo en cada arranque.
 *
 * <p>También marca como administradores los correos de ADMINISTRADORES, separados por comas.
 * No le quita el rol a nadie: eso se hace a mano en la base.
 */
@Component
public class InicializadorBaseDatos implements InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(InicializadorBaseDatos.class);

  private final String administradores;

  public InicializadorBaseDatos(@Value("${ADMINISTRADORES:}") String administradores) {
    this.administradores = administradores;
  }

  @Override
  public void afterPropertiesSet() {
    ConexionBaseDatos conexion = ConexionBaseDatos.desdeEntorno();
    SingleConnectionDataSource fuente = new SingleConnectionDataSource(
        conexion.jdbcUrl(), conexion.usuario(), conexion.clave(), true);
    try {
      new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(fuente);
      marcarAdministradores(new JdbcTemplate(fuente));
      log.info("Base de datos lista.");
    } finally {
      fuente.destroy();
    }
  }

  private void marcarAdministradores(JdbcTemplate jdbc) {
    List<String> correos = Arrays.stream(administradores.split(","))
        .map((correo) -> correo.trim().toLowerCase())
        .filter((correo) -> !correo.isEmpty())
        .toList();
    for (String correo : correos) {
      jdbc.update("UPDATE usuarios SET rol = 'admin' WHERE lower(email) = ?", correo);
    }
    if (!correos.isEmpty()) log.info("Administradores revisados: {}", correos.size());
  }
}
