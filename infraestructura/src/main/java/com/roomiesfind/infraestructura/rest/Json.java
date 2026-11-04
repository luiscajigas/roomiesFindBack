package com.roomiesfind.infraestructura.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Lectura tolerante del JSON que llega del frontend. */
final class Json {

  private Json() {}

  static JsonNode objetoOVacio(JsonNode cuerpo) {
    return cuerpo != null && cuerpo.isObject() ? cuerpo : JsonNodeFactory.instance.objectNode();
  }

  static boolean tieneValor(JsonNode objeto, String campo) {
    JsonNode valor = objeto.get(campo);
    return valor != null && !valor.isNull();
  }

  /** Acepta números y textos numéricos; cualquier otra cosa queda como NaN y la rechaza el servicio. */
  static double comoNumero(JsonNode valor) {
    if (valor.isNumber()) return valor.doubleValue();
    if (valor.isTextual()) {
      try {
        return Double.parseDouble(valor.textValue().trim());
      } catch (NumberFormatException error) {
        return Double.NaN;
      }
    }
    return Double.NaN;
  }

  static boolean comoBooleano(JsonNode valor) {
    if (valor.isBoolean()) return valor.booleanValue();
    if (valor.isTextual() && "true".equals(valor.textValue())) return true;
    if (valor.isTextual() && "false".equals(valor.textValue())) return false;
    throw new ErrorRest(HttpStatus.BAD_REQUEST, "La solicitud no es válida.");
  }

  static List<String> comoListaDeTexto(JsonNode valor) {
    List<String> lista = new ArrayList<>();
    if (valor != null && valor.isArray()) valor.forEach((item) -> lista.add(item.asText()));
    return lista;
  }
}
