package com.roomiesfind.perfiles.controller;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.perfiles.dto.DatosPerfilDto;
import com.roomiesfind.perfiles.dto.FotoDto;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Reglas de validación de los datos de un perfil. */
public final class ReglasPerfil {

  private static final List<String> HORARIOS = List.of("madrugador", "nocturno", "mixto");
  private static final List<String> FRECUENCIAS = List.of("nunca", "ocasional", "frecuente");
  private static final Set<String> TIPOS_FOTO = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
  private static final Pattern RUTA_UPLOADS = Pattern.compile("^/uploads/[A-Za-z0-9._-]+$");
  private static final long MAX_PRESUPUESTO = Integer.MAX_VALUE;
  private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

  private ReglasPerfil() {}

  public static void validarPreferencias(DatosPerfilDto datos) {
    if (datos.presupuesto() != null && !esEnteroEntre(datos.presupuesto(), 0, MAX_PRESUPUESTO)) {
      throw invalida("El presupuesto debe ser un número entero positivo.");
    }
    if (datos.horario() != null && !HORARIOS.contains(datos.horario())) {
      throw invalida("El horario no es válido.");
    }
    if (datos.limpieza() != null && !esEnteroEntre(datos.limpieza(), 1, 5)) {
      throw invalida("El nivel de limpieza debe estar entre 1 y 5.");
    }
    if (datos.toleranciaRuido() != null && !esEnteroEntre(datos.toleranciaRuido(), 1, 5)) {
      throw invalida("La tolerancia al ruido debe estar entre 1 y 5.");
    }
    if (datos.frecuenciaVisitas() != null && !FRECUENCIAS.contains(datos.frecuenciaVisitas())) {
      throw invalida("La frecuencia de visitas no es válida.");
    }
    if (datos.descripcion() != null && datos.descripcion().length() > 1000) {
      throw invalida("La descripción puede tener máximo 1000 caracteres.");
    }
  }

  public static void validarFoto(FotoDto foto) {
    if (!TIPOS_FOTO.contains(foto.tipoMime())) {
      throw invalida("Elige una imagen JPG, PNG, WEBP o GIF.");
    }
    if (!esImagenReal(foto)) {
      throw invalida("El archivo seleccionado no es una imagen válida.");
    }
  }

  public static boolean esUrlImagenValida(String valor) {
    if (valor.length() > 2048) return false;
    if (RUTA_UPLOADS.matcher(valor).matches()) return true;
    try {
      URI url = URI.create(valor);
      return ("http".equals(url.getScheme()) || "https".equals(url.getScheme())) && url.getHost() != null;
    } catch (IllegalArgumentException error) {
      return false;
    }
  }

  static boolean esEnteroEntre(String valor, long minimo, long maximo) {
    try {
      BigDecimal numero = new BigDecimal(valor.trim());
      return numero.stripTrailingZeros().scale() <= 0
          && numero.compareTo(BigDecimal.valueOf(minimo)) >= 0
          && numero.compareTo(BigDecimal.valueOf(maximo)) <= 0;
    } catch (NumberFormatException error) {
      return false;
    }
  }

  /** Comprueba los primeros bytes: el tipo que declara el navegador no basta. */
  static boolean esImagenReal(FotoDto foto) {
    byte[] contenido = foto.contenido();
    return switch (foto.tipoMime()) {
      case "image/jpeg" -> contenido.length >= 3
          && contenido[0] == (byte) 0xff && contenido[1] == (byte) 0xd8 && contenido[2] == (byte) 0xff;
      case "image/png" -> contenido.length >= 8 && Arrays.equals(Arrays.copyOf(contenido, 8), FIRMA_PNG);
      case "image/webp" -> contenido.length >= 12 && "RIFF".equals(ascii(contenido, 0, 4)) && "WEBP".equals(ascii(contenido, 8, 4));
      case "image/gif" -> contenido.length >= 6 && List.of("GIF87a", "GIF89a").contains(ascii(contenido, 0, 6));
      default -> false;
    };
  }

  private static String ascii(byte[] contenido, int inicio, int largo) {
    return new String(contenido, inicio, largo, StandardCharsets.US_ASCII);
  }

  private static ErrorNegocio invalida(String mensaje) {
    return ErrorNegocio.solicitudInvalida(mensaje);
  }
}
