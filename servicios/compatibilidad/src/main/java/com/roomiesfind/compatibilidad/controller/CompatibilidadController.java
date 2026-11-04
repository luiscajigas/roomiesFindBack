package com.roomiesfind.compatibilidad.controller;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.compatibilidad.dto.ExplicacionDto;
import com.roomiesfind.compatibilidad.dto.SolicitudExplicacionDto;
import com.roomiesfind.compatibilidad.service.CompatibilidadService;
import org.springframework.stereotype.Component;

/** Valida la entrada y coordina los casos de uso. No conoce gRPC ni SQL. */
@Component
public class CompatibilidadController {

  private final CompatibilidadService servicio;

  public CompatibilidadController(CompatibilidadService servicio) {
    this.servicio = servicio;
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

  static boolean esIdValido(double id) {
    return id > 0 && id <= Integer.MAX_VALUE && id == Math.rint(id);
  }

  static boolean esPuntajeValido(double score) {
    return score >= 0 && score <= 100 && score == Math.rint(score);
  }
}
