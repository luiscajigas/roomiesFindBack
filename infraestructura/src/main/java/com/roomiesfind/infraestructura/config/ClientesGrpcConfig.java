package com.roomiesfind.infraestructura.config;

import com.roomiesfind.comun.CanalGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Direcciones de los microservicios. Todas se conectan por gRPC sobre HTTP/2. */
@Configuration
public class ClientesGrpcConfig {

  @Bean(destroyMethod = "shutdown")
  ManagedChannel canalUsuarios(@Value("${GRPC_USUARIOS:localhost:9091}") String destino) {
    return CanalGrpc.hacia(destino);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel canalPerfiles(@Value("${GRPC_PERFILES:localhost:9092}") String destino) {
    return CanalGrpc.hacia(destino);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel canalCompatibilidad(@Value("${GRPC_COMPATIBILIDAD:localhost:9093}") String destino) {
    return CanalGrpc.hacia(destino);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel canalChat(@Value("${GRPC_CHAT:localhost:9095}") String destino) {
    return CanalGrpc.hacia(destino);
  }

  @Bean(destroyMethod = "shutdown")
  ManagedChannel canalModeracion(@Value("${GRPC_MODERACION:localhost:9096}") String destino) {
    return CanalGrpc.hacia(destino);
  }

  @Bean
  ClientesGrpc clientesGrpc(ManagedChannel canalUsuarios, ManagedChannel canalPerfiles, ManagedChannel canalCompatibilidad,
      ManagedChannel canalChat, ManagedChannel canalModeracion) {
    return new ClientesGrpc(canalUsuarios, canalPerfiles, canalCompatibilidad, canalChat, canalModeracion);
  }
}
