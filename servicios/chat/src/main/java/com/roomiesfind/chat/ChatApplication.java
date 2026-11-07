package com.roomiesfind.chat;

import com.roomiesfind.chat.api.ChatApi;
import com.roomiesfind.chat.eventos.SuscriptoresEventos;
import com.roomiesfind.comun.CanalGrpc;
import com.roomiesfind.comun.ConexionBaseDatos;
import com.roomiesfind.comun.ServidorGrpc;
import com.roomiesfind.contratos.moderacion.ModeracionGrpc;
import com.zaxxer.hikari.HikariDataSource;
import io.grpc.ManagedChannel;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@SpringBootApplication
public class ChatApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChatApplication.class, args);
  }

  @Bean
  ServidorGrpc servidorGrpc(@Value("${GRPC_PUERTO:9095}") int puerto, ChatApi api) {
    return new ServidorGrpc(puerto, api);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel canalModeracion(@Value("${GRPC_MODERACION:localhost:9096}") String destino) {
    return CanalGrpc.hacia(destino);
  }

  @Bean
  ModeracionGrpc.ModeracionBlockingStub moderacionGrpc(ManagedChannel canalModeracion) {
    return ModeracionGrpc.newBlockingStub(canalModeracion);
  }

  /** Recibe de Redis los eventos de todas las personas y los reparte a los streams abiertos aquí. */
  @Bean
  RedisMessageListenerContainer escuchaRedis(RedisConnectionFactory conexion, SuscriptoresEventos suscriptores) {
    RedisMessageListenerContainer contenedor = new RedisMessageListenerContainer();
    contenedor.setConnectionFactory(conexion);
    contenedor.addMessageListener(suscriptores, new PatternTopic(SuscriptoresEventos.PATRON_CANALES));
    return contenedor;
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
