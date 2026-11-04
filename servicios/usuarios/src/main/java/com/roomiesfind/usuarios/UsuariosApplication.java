package com.roomiesfind.usuarios;

import com.roomiesfind.comun.ConexionBaseDatos;
import com.roomiesfind.comun.ServidorGrpc;
import com.roomiesfind.usuarios.api.UsuariosApi;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UsuariosApplication {

  public static void main(String[] args) {
    SpringApplication.run(UsuariosApplication.class, args);
  }

  @Bean
  ServidorGrpc servidorGrpc(@Value("${GRPC_PUERTO:9091}") int puerto, UsuariosApi api) {
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
