package com.roomiesfind.infraestructura.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class WebConfigTest {

  @Test
  void aceptaVariosOrigenesConfigurados() {
    assertEquals(List.of("http://localhost:4300", "https://otro.com"),
        WebConfig.origenesPermitidos("http://localhost:4300, https://otro.com"));
  }

  @Test
  void aceptaLasVistasPreviasDelProyectoDeVercel() {
    assertEquals(List.of("https://roomies.vercel.app", "https://roomies-*.vercel.app"),
        WebConfig.origenesPermitidos("https://roomies.vercel.app"));
  }

  @Test
  void sinFrontendConfiguradoNoAceptaNinguno() {
    assertEquals(List.of(), WebConfig.origenesPermitidos(""));
  }
}
