package com.roomiesfind.ia.controller;

import com.roomiesfind.ia.dto.SolicitudTextoDto;
import com.roomiesfind.ia.service.ExplicacionService;
import org.springframework.stereotype.Component;

/** Punto de entrada a la capa interna. No conoce gRPC ni los proveedores de IA. */
@Component
public class ExplicacionController {

  private final ExplicacionService servicio;

  public ExplicacionController(ExplicacionService servicio) {
    this.servicio = servicio;
  }

  public String generar(SolicitudTextoDto solicitud) {
    return servicio.generar(solicitud);
  }
}
