package com.roomiesfind.ia.service;

import com.roomiesfind.ia.dto.SolicitudTextoDto;
import java.util.List;
import java.util.stream.Collectors;

/** Arma la instrucción para la IA. La IA interpreta el puntaje; nunca lo decide. */
final class PromptExplicacion {

  private static final int MAX_ITEMS_LISTA = 10;

  private PromptExplicacion() {}

  static String construir(SolicitudTextoDto solicitud) {
    String usuario = solicitud.nombreUsuario();
    String candidato = solicitud.nombreCandidato();
    return String.join("\n",
        "Eres el asistente de RoomiesFind, una aplicación para encontrar compañeros de vivienda.",
        "Explica en español, dirigida a " + usuario + ", por qué " + usuario + " y " + candidato
            + " tienen un " + solicitud.score() + "% de compatibilidad.",
        "El puntaje ya fue calculado con reglas objetivas; tu único trabajo es interpretarlo, no modificarlo.",
        "",
        "Puntos en común:",
        comoLista(solicitud.coincidencias()),
        "",
        "Posibles temas para conversar antes de vivir juntos:",
        comoLista(solicitud.conflictos()),
        "",
        "Empieza directamente con la explicación: no escribas el nombre de la aplicación, encabezados ni saludos.",
        "Estructura: primero lo que comparten y por qué facilita la convivencia, luego lo que conviene acordar, y termina con un consejo práctico y concreto.",
        "Escribe entre 90 y 130 palabras en texto corrido, sin títulos, listas ni formato markdown.",
        "Usa un tono cercano y respetuoso. No inventes datos que no estén en las listas.",
        "La respuesta debe terminar con una oración completa.");
  }

  static String comoLista(List<String> items) {
    String lista = items.stream()
        .limit(MAX_ITEMS_LISTA)
        .map(String::trim)
        .filter((item) -> !item.isEmpty())
        .map((item) -> "- " + item)
        .collect(Collectors.joining("\n"));
    return lista.isEmpty() ? "- (sin datos registrados)" : lista;
  }
}
