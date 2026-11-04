package com.roomiesfind.infraestructura.config;

import com.roomiesfind.comun.ConexionBaseDatos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Crea las tablas que falten antes de recibir peticiones. Todos los servicios
 * comparten una sola base; es seguro ejecutarlo en cada arranque.
 */
@Component
public class InicializadorBaseDatos implements InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(InicializadorBaseDatos.class);

  @Override
  public void afterPropertiesSet() {
    ConexionBaseDatos conexion = ConexionBaseDatos.desdeEntorno();
    SingleConnectionDataSource fuente = new SingleConnectionDataSource(
        conexion.jdbcUrl(), conexion.usuario(), conexion.clave(), true);
    try {
      new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(fuente);
      log.info("Base de datos lista.");
    } finally {
      fuente.destroy();
    }
  }
}
