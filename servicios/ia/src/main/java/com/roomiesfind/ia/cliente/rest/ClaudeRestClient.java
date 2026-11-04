package com.roomiesfind.ia.cliente.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomiesfind.comun.Entorno;
import com.roomiesfind.ia.model.ProveedorIa;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** API REST de Anthropic (POST /v1/messages). Es el proveedor principal. */
@Component
@Order(1)
public class ClaudeRestClient implements ProveedorIa {

  private static final String MODELO_POR_DEFECTO = "claude-opus-5-5";
  private static final String VERSION_API = "2026-10-21";
  private static final String BETA_RESPALDO = "server-side-fallback-2026-07-01";
  private static final int MAX_INTENTOS = 3;
  private static final long ESPERA_BASE_MS = 500;

  private final String apiKey = Entorno.opcional("ANTHROPIC_API_KEY", null);
  private final String modelo = Entorno.opcional("ANTHROPIC_MODEL", MODELO_POR_DEFECTO);
  private final ClienteRest rest = new ClienteRest(
      Entorno.opcional("ANTHROPIC_BASE_URL", "https://api.anthropic.com"), Duration.ofSeconds(30));

  @Override
  public String nombre() {
    return "Claude";
  }

  @Override
  public boolean estaConfigurado() {
    return apiKey != null;
  }

  @Override
  public String generar(String prompt) {
    Map<String, String> encabezados = new HashMap<>();
    encabezados.put("x-api-key", apiKey);
    encabezados.put("anthropic-version", VERSION_API);

    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("model", modelo);
    cuerpo.put("max_tokens", 4000);
    cuerpo.put("output_config", Map.of("effort", "low"));
    cuerpo.put("messages", List.of(Map.of("role", "user", "content", prompt)));
    // Si los filtros de seguridad rechazan la petición, pasa a otro modelo. Haiku no lo admite.
    if (!modelo.startsWith("claude-haiku")) {
      encabezados.put("anthropic-beta", BETA_RESPALDO);
      cuerpo.put("fallbacks", "default");
    }

    return leerTexto(llamarConReintentos(encabezados, cuerpo));
  }

  private JsonNode llamarConReintentos(Map<String, String> encabezados, Map<String, Object> cuerpo) {
    for (int intento = 1; ; intento++) {
      try {
        return rest.post("/v1/messages", encabezados, cuerpo);
      } catch (ClienteRest.ErrorRest error) {
        if (!error.esTransitorio() || intento == MAX_INTENTOS) throw error;
        ClienteRest.esperar(ESPERA_BASE_MS * (1L << (intento - 1)));
      }
    }
  }

  static String leerTexto(JsonNode respuesta) {
    if ("refusal".equals(respuesta.path("stop_reason").asText())) {
      throw new IllegalStateException("Claude rechazó generar la explicación.");
    }
    StringBuilder texto = new StringBuilder();
    for (JsonNode bloque : respuesta.path("content")) {
      if ("text".equals(bloque.path("type").asText())) texto.append(bloque.path("text").asText());
    }
    String resultado = texto.toString().trim();
    if (resultado.isEmpty()) throw new IllegalStateException("Claude no devolvió texto.");
    return resultado;
  }
}
