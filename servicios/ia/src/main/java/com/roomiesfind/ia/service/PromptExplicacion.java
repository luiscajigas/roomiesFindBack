package com.roomiesfind.ia.service;

import com.roomiesfind.ia.dto.SolicitudTextoDto;
import java.util.List;
import java.util.stream.Collectors;

/** Arma la instrucción para la IA. La IA interpreta el puntaje; nunca lo decide. */
final class PromptExplicacion {

  private static final int MAX_ITEMS_LISTA = 10;

  private PromptExplicacion() {}

  static String construir(SolicitudTextoDto solicitud) {
    return "en".equals(solicitud.idioma()) ? enIngles(solicitud) : enEspanol(solicitud);
  }

  private static String enIngles(SolicitudTextoDto solicitud) {
    String usuario = solicitud.nombreUsuario();
    String candidato = solicitud.nombreCandidato();
    return String.join("\n",
        "You are the assistant of RoomiesFind, an app for finding roommates.",
        "Explain in English, addressed to " + usuario + ", why " + usuario + " and " + candidato
            + " have a " + solicitud.score() + "% compatibility.",
        "The score was already calculated with objective rules; your only job is to interpret it, not change it.",
        "The lists below are in Spanish: translate them as you use them.",
        "",
        "Things in common:",
        comoLista(solicitud.coincidencias()),
        "",
        "Possible topics to discuss before living together:",
        comoLista(solicitud.conflictos()),
        "",
        "Start directly with the explanation: do not write the app name, headings or greetings.",
        "Structure: first what they share and why it makes living together easier, then what they should agree on, and end with a practical, specific tip.",
        "Write between 90 and 130 words of flowing text, without titles, lists or markdown.",
        "Use a warm, respectful tone. Do not invent facts that are not in the lists.",
        "The response must end with a complete sentence.");
  }

  private static String enEspanol(SolicitudTextoDto solicitud) {
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
