package com.roomiesfind.compatibilidad;

import com.roomiesfind.comun.CanalGrpc;
import com.roomiesfind.comun.ConexionBaseDatos;
import com.roomiesfind.comun.ServidorGrpc;
import com.roomiesfind.compatibilidad.api.CompatibilidadApi;
import com.roomiesfind.contratos.ia.IaGrpc;
import com.roomiesfind.contratos.usuarios.UsuariosGrpc;
import com.zaxxer.hikari.HikariDataSource;
import io.grpc.ManagedChannel;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CompatibilidadApplication {

  public static void main(String[] args) {
    SpringApplication.run(CompatibilidadApplication.class, args);
  }

  @Bean
  ServidorGrpc servidorGrpc(@Value("${GRPC_PUERTO:9093}") int puerto, CompatibilidadApi api) {
    return new ServidorGrpc(puerto, api);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel canalUsuarios(@Value("${GRPC_USUARIOS:localhost:9091}") String destino) {
    return CanalGrpc.hacia(destino);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel canalIa(@Value("${GRPC_IA:localhost:9094}") String destino) {
    return CanalGrpc.hacia(destino);
  }

  @Bean
  UsuariosGrpc.UsuariosBlockingStub usuariosGrpc(ManagedChannel canalUsuarios) {
    return UsuariosGrpc.newBlockingStub(canalUsuarios);
  }

  @Bean
  IaGrpc.IaBlockingStub iaGrpc(ManagedChannel canalIa) {
    return IaGrpc.newBlockingStub(canalIa);
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
