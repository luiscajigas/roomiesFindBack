package com.roomiesfind.ia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.ia.dto.SolicitudTextoDto;
import com.roomiesfind.ia.model.ProveedorIa;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExplicacionServiceTest {

  private static final SolicitudTextoDto SOLICITUD =
      new SolicitudTextoDto("Ana", "Beto", 80, List.of("Horarios"), List.of(), "es");

  private record Falso(String nombre, boolean configurado, String texto, List<String> llamados) implements ProveedorIa {
    @Override public boolean estaConfigurado() { return configurado; }
    @Override public String generar(String prompt) {
      llamados.add(nombre);
      if (texto == null) throw new IllegalStateException(nombre + " saturado");
      return texto;
    }
  }

  @Test
  void usaElProveedorPrincipalSiResponde() {
    List<String> llamados = new ArrayList<>();
    ExplicacionService servicio = new ExplicacionService(List.of(
        new Falso("Claude", true, "Desde Claude.", llamados), new Falso("Gemini", true, "Desde Gemini.", llamados)));
    assertEquals("Desde Claude.", servicio.generar(SOLICITUD));
    assertEquals(List.of("Claude"), llamados);
  }

  @Test
  void pasaAlRespaldoSiElPrincipalFalla() {
    List<String> llamados = new ArrayList<>();
    ExplicacionService servicio = new ExplicacionService(List.of(
        new Falso("Claude", true, null, llamados), new Falso("Gemini", true, "Desde Gemini.", llamados)));
    assertEquals("Desde Gemini.", servicio.generar(SOLICITUD));
    assertEquals(List.of("Claude", "Gemini"), llamados);
  }

  @Test
  void ignoraLosProveedoresSinClave() {
    List<String> llamados = new ArrayList<>();
    ExplicacionService servicio = new ExplicacionService(List.of(
        new Falso("Claude", false, "Desde Claude.", llamados), new Falso("Gemini", true, "Desde Gemini.", llamados)));
    assertEquals("Desde Gemini.", servicio.generar(SOLICITUD));
  }

  @Test
  void avisaSiNoHayClavesYPropagaElUltimoError() {
    assertThrows(ErrorNegocio.class, () -> new ExplicacionService(List.of()).generar(SOLICITUD));
    assertThrows(IllegalStateException.class, () -> new ExplicacionService(List.of(
        new Falso("Claude", true, null, new ArrayList<>()))).generar(SOLICITUD));
  }

  @Test
  void elPromptIncluyeLosDatosYNoInventaListas() {
    String prompt = PromptExplicacion.construir(SOLICITUD);
    assertTrue(prompt.contains("Ana y Beto tienen un 80% de compatibilidad"));
    assertTrue(prompt.contains("- Horarios"));
    assertTrue(prompt.contains("- (sin datos registrados)"));
  }

  @Test
  void escribeElPromptEnElIdiomaPedido() {
    String ingles = PromptExplicacion.construir(new SolicitudTextoDto("Ana", "Beto", 80, List.of("Horarios"), List.of(), "en"));
    assertTrue(ingles.contains("Explain in English"));
    assertTrue(ingles.contains("Ana and Beto have a 80% compatibility"));
    assertFalse(ingles.contains("Explica en español"));

    String sinIdioma = PromptExplicacion.construir(new SolicitudTextoDto("Ana", "Beto", 80, List.of(), List.of(), ""));
    assertTrue(sinIdioma.contains("Explica en español"));
  }

  @Test
  void limitaLasListasDelPrompt() {
    List<String> muchos = new ArrayList<>();
    for (int i = 0; i < 20; i++) muchos.add("punto " + i);
    String lista = PromptExplicacion.comoLista(muchos);
    assertTrue(lista.contains("punto 9"));
    assertFalse(lista.contains("punto 10"));
  }

  @Test
  void cortaEnLaUltimaOracionCompleta() {
    String completo = "Esta es una oración suficientemente larga para pasar el mínimo.";
    assertEquals(completo, RecorteTexto.hastaUltimaOracionCompleta(completo));
    assertEquals(completo, RecorteTexto.hastaUltimaOracionCompleta(completo + " Y esta quedó a"));
    assertEquals("Texto corto sin", RecorteTexto.hastaUltimaOracionCompleta("Texto corto sin"));
  }
}
