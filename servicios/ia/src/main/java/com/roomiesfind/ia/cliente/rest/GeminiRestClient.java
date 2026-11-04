package com.roomiesfind.ia.cliente.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomiesfind.comun.Entorno;
import com.roomiesfind.ia.model.ProveedorIa;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** API REST de Google Gemini. Es el proveedor de respaldo. */
@Component
@Order(2)
public class GeminiRestClient implements ProveedorIa {

  private static final List<String> MODELOS_POR_DEFECTO = List.of("gemini-3.8-flash", "gemini-2.5-flash");
  private static final int MAX_INTENTOS_POR_MODELO = 3;
  private static final long ESPERA_BASE_MS = 1200;

  private final String apiKey = Entorno.opcional("GEMINI_API_KEY", null);
  private final String modeloConfigurado = Entorno.opcional("GEMINI_MODEL", null);
  private final ClienteRest rest = new ClienteRest(
      Entorno.opcional("GEMINI_BASE_URL", "https://generativelanguage.googleapis.com"), Duration.ofSeconds(20));

  @Override
  public String nombre() {
    return "Gemini";
  }

  @Override
  public boolean estaConfigurado() {
    return apiKey != null;
  }

  /** Reintenta ante 429, 503 o falta de respuesta; prueba otro modelo si el configurado ya no existe. */
  @Override
  public String generar(String prompt) {
    RuntimeException ultimoError = null;
    for (String modelo : modelos()) {
      for (int intento = 1; intento <= MAX_INTENTOS_POR_MODELO; intento++) {
        try {
          return llamar(modelo, prompt);
        } catch (ClienteRest.ErrorRest error) {
          ultimoError = error;
          boolean transitorio = error.status == 0 || error.status == 429 || error.status == 503;
          if (!transitorio || intento == MAX_INTENTOS_POR_MODELO) break;
          ClienteRest.esperar(ESPERA_BASE_MS * intento);
        }
      }
    }
    throw ultimoError;
  }

  private List<String> modelos() {
    List<String> modelos = new ArrayList<>();
    if (modeloConfigurado != null) modelos.add(modeloConfigurado);
    MODELOS_POR_DEFECTO.stream().filter((modelo) -> !modelo.equals(modeloConfigurado)).forEach(modelos::add);
    return modelos;
  }

  private String llamar(String modelo, String prompt) {
    Map<String, Object> cuerpo = Map.of(
        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
        "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 1024));
    // La clave va en un encabezado para que no aparezca en URLs ni en logs.
    JsonNode respuesta = rest.post(
        "/v1beta/models/" + URLEncoder.encode(modelo, StandardCharsets.UTF_8) + ":generateContent",
        Map.of("x-goog-api-key", apiKey), cuerpo);
    return leerTexto(respuesta);
  }

  static String leerTexto(JsonNode respuesta) {
    StringBuilder texto = new StringBuilder();
    for (JsonNode parte : respuesta.path("candidates").path(0).path("content").path("parts")) {
      texto.append(parte.path("text").asText(""));
    }
    String resultado = texto.toString().trim();
    if (resultado.isEmpty()) throw new IllegalStateException("El proveedor de IA no devolvió texto.");
    return resultado;
  }
}
