package com.roomiesfind.infraestructura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Orquestador del sistema. Es la única puerta de entrada para el frontend (REST)
 * y coordina a los microservicios, siempre por gRPC.
 */
@SpringBootApplication
public class InfraestructuraApplication {

  public static void main(String[] args) {
    SpringApplication.run(InfraestructuraApplication.class, args);
  }
}
