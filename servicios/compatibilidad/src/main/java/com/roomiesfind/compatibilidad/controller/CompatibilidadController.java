package com.roomiesfind.compatibilidad.controller;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.compatibilidad.dto.ExplicacionDto;
import com.roomiesfind.compatibilidad.dto.SolicitudExplicacionDto;
import com.roomiesfind.compatibilidad.service.CompatibilidadService;
import com.roomiesfind.compatibilidad.service.TraduccionService;
import org.springframework.stereotype.Component;

/** Valida la entrada y coordina los casos de uso. No conoce gRPC ni SQL. */
@Component
public class CompatibilidadController {

  private static final int LARGO_MAXIMO_TEXTO = 1000;

  private final CompatibilidadService servicio;
  private final TraduccionService traducciones;

  public CompatibilidadController(CompatibilidadService servicio, TraduccionService traducciones) {
    this.servicio = servicio;
    this.traducciones = traducciones;
  }

  public void registrar(int usuarioId, Double candidatoId, Double score) {
    if (candidatoId == null || candidatoId == 0 || score == null) {
      throw ErrorNegocio.solicitudInvalida("candidatoId y score son obligatorios.");
    }
    if (!esIdValido(candidatoId) || candidatoId.intValue() == usuarioId || !esPuntajeValido(score)) {
      throw ErrorNegocio.solicitudInvalida("candidatoId o score no son válidos.");
    }
    servicio.registrar(usuarioId, candidatoId.intValue(), score.intValue());
  }

  public ExplicacionDto explicar(SolicitudExplicacionDto solicitud) {
    if (!esIdValido(solicitud.candidatoId()) || !esPuntajeValido(solicitud.score())) {
      throw ErrorNegocio.solicitudInvalida("candidatoId y score son obligatorios y deben ser válidos.");
    }
    if ((int) solicitud.candidatoId() == solicitud.usuarioId()) {
      throw ErrorNegocio.solicitudInvalida("No puedes pedir una explicación de compatibilidad contigo mismo.");
    }
    return servicio.explicar(solicitud);
  }

  public String traducir(String texto, String idioma) {
    if (!"en".equals(idioma) && !"es".equals(idioma)) {
      throw ErrorNegocio.solicitudInvalida("El idioma no es válido.");
    }
    if (texto.isBlank() || texto.length() > LARGO_MAXIMO_TEXTO) {
      throw ErrorNegocio.solicitudInvalida("El texto no se puede traducir.");
    }
    return traducciones.traducir(texto, idioma);
  }

  static boolean esIdValido(double id) {
    return id > 0 && id <= Integer.MAX_VALUE && id == Math.rint(id);
  }

  static boolean esPuntajeValido(double score) {
    return score >= 0 && score <= 100 && score == Math.rint(score);
  }
}
