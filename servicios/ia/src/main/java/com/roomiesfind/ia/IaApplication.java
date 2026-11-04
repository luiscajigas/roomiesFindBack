package com.roomiesfind.ia;

import com.roomiesfind.comun.ServidorGrpc;
import com.roomiesfind.ia.api.IaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class IaApplication {

  public static void main(String[] args) {
    SpringApplication.run(IaApplication.class, args);
  }

  @Bean
  ServidorGrpc servidorGrpc(@Value("${GRPC_PUERTO:9094}") int puerto, IaApi api) {
    return new ServidorGrpc(puerto, api);
  }
}
