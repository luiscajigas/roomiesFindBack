package com.roomiesfind.infraestructura.rest;

import com.roomiesfind.infraestructura.contrato.OtrasRespuestas.Salud;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Para saber si el orquestador está arriba. No consulta a los microservicios. */
@RestController
public class SaludRest {

  @GetMapping("/api/salud")
  Salud salud() {
    return new Salud("ok", "roomies-backend");
  }
}
