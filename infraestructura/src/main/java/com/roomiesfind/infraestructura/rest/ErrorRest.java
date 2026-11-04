package com.roomiesfind.infraestructura.rest;

import org.springframework.http.HttpStatus;

/** Error que se responde tal cual al frontend. */
public class ErrorRest extends RuntimeException {

  private final HttpStatus status;

  public ErrorRest(HttpStatus status, String mensaje) {
    super(mensaje);
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }
}
