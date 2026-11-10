package com.roomiesfind.infraestructura.contrato;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Respuestas pequeñas que no merecen un archivo propio. */
public final class OtrasRespuestas {

  private OtrasRespuestas() {}

  /** Formato de todos los errores de la API. El mensaje está en español y se puede mostrar. */
  public record ErrorRespuesta(String error) {}

  public record Salud(String estado, String servicio) {}

  public record FotoGuardada(@JsonProperty("foto_url") String fotoUrl) {}

  public record Registrado(boolean registrado) {}

  public record Traduccion(String descripcion) {}

  public record ReporteCreado(boolean alertaActivada) {}

  public record Explicacion(int id, String estado, String explicacion, int score, List<String> coincidencias,
      List<String> conflictos, boolean desdeCache) {}
}
