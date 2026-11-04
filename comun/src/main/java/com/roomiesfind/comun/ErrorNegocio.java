package com.roomiesfind.comun;

import io.grpc.Status;

/** Error esperado de una regla de negocio. Su mensaje lo puede leer el usuario. */
public class ErrorNegocio extends RuntimeException {

  private final transient Status status;

  private ErrorNegocio(Status status, String mensaje) {
    super(mensaje);
    this.status = status;
  }

  public static ErrorNegocio solicitudInvalida(String mensaje) {
    return new ErrorNegocio(Status.INVALID_ARGUMENT, mensaje);
  }

  public static ErrorNegocio noAutenticado(String mensaje) {
    return new ErrorNegocio(Status.UNAUTHENTICATED, mensaje);
  }

  public static ErrorNegocio noEncontrado(String mensaje) {
    return new ErrorNegocio(Status.NOT_FOUND, mensaje);
  }

  public static ErrorNegocio conflicto(String mensaje) {
    return new ErrorNegocio(Status.ALREADY_EXISTS, mensaje);
  }

  /** Un recurso externo falló. UNAVAILABLE queda reservado para los fallos de red entre servicios. */
  public static ErrorNegocio noDisponible(String mensaje) {
    return new ErrorNegocio(Status.FAILED_PRECONDITION, mensaje);
  }

  public Status comoStatus() {
    return status.withDescription(getMessage());
  }
}
