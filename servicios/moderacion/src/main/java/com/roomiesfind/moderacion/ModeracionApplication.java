package com.roomiesfind.moderacion;

import com.roomiesfind.comun.ConexionBaseDatos;
import com.roomiesfind.comun.ServidorGrpc;
import com.roomiesfind.moderacion.api.ModeracionApi;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ModeracionApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModeracionApplication.class, args);
  }

  @Bean
  ServidorGrpc servidorGrpc(@Value("${GRPC_PUERTO:9096}") int puerto, ModeracionApi api) {
    return new ServidorGrpc(puerto, api);
  }

  @Bean
  DataSource baseDatos() {
    ConexionBaseDatos conexion = ConexionBaseDatos.desdeEntorno();
    HikariDataSource fuente = new HikariDataSource();
    fuente.setJdbcUrl(conexion.jdbcUrl());
    fuente.setUsername(conexion.usuario());
    fuente.setPassword(conexion.clave());
    fuente.setMaximumPoolSize(3);
    fuente.setMinimumIdle(0);
    return fuente;
  }
}
