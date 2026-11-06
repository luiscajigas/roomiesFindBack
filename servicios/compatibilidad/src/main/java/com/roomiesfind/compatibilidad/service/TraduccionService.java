package com.roomiesfind.compatibilidad.service;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.compatibilidad.cliente.IaCliente;
import com.roomiesfind.compatibilidad.repository.TraduccionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Traduce descripciones una sola vez: cada texto se guarda para no volver a pagarlo. */
@Service
public class TraduccionService {

  private static final Logger log = LoggerFactory.getLogger(TraduccionService.class);

  private final TraduccionRepository traducciones;
  private final IaCliente ia;

  public TraduccionService(TraduccionRepository traducciones, IaCliente ia) {
    this.traducciones = traducciones;
    this.ia = ia;
  }

  public String traducir(String texto, String idioma) {
    String huella = huella(texto);
    try {
      Optional<String> guardada = traducciones.buscar(huella, idioma);
      if (guardada.isPresent()) return guardada.get();

      String traduccion = ia.traducir(texto, idioma);
      traducciones.guardar(huella, idioma, traduccion);
      return traduccion;
    } catch (RuntimeException error) {
      log.error("No se pudo traducir la descripción: {}", error.getMessage());
      throw ErrorNegocio.noDisponible("La traducción no está disponible en este momento.");
    }
  }

  static String huella(String texto) {
    try {
      byte[] resumen = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(resumen);
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }
}
