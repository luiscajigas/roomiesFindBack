package com.roomiesfind.ia.cliente.rest;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Llamada POST a una API de terceros por REST. Es el único tipo de comunicación
 * hacia afuera del sistema; entre microservicios todo va por gRPC.
 */
final class ClienteRest {

  /** Error de una API externa. El mensaje nunca incluye claves ni la URL completa. */
  static final class ErrorRest extends RuntimeException {
    final int status;

    ErrorRest(int status, String mensaje) {
      super(mensaje);
      this.status = status;
    }

    boolean esTransitorio() {
      return status == 0 || status == 408 || status == 409 || status == 429 || status >= 500;
    }
  }

  private final RestClient cliente;

  ClienteRest(String urlBase, Duration espera) {
    JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    fabrica.setReadTimeout(espera);
    this.cliente = RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
  }

  JsonNode post(String ruta, Map<String, String> encabezados, Object cuerpo) {
    try {
      return cliente.post()
          .uri(ruta)
          .contentType(MediaType.APPLICATION_JSON)
          .headers((http) -> encabezados.forEach(http::set))
          .body(cuerpo)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientResponseException error) {
      String detalle = error.getResponseBodyAsString();
      throw new ErrorRest(error.getStatusCode().value(), "El proveedor de IA respondió "
          + error.getStatusCode().value() + ": " + detalle.substring(0, Math.min(detalle.length(), 200)));
    } catch (ResourceAccessException error) {
      throw new ErrorRest(0, "No se pudo contactar con el proveedor de IA.");
    }
  }

  static void esperar(long milisegundos) {
    try {
      Thread.sleep(milisegundos);
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new ErrorRest(0, "Se interrumpió la espera del proveedor de IA.");
    }
  }
}
