package com.roomiesfind.infraestructura.rest;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaludRest {

  @GetMapping("/api/salud")
  Map<String, String> salud() {
    return Map.of("estado", "ok", "servicio", "roomies-backend");
  }
}
