package com.roomiesfind.comun;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responde una llamada gRPC y convierte las excepciones en estados gRPC. */
public final class RespuestaGrpc {

  private static final Logger log = LoggerFactory.getLogger(RespuestaGrpc.class);

  private RespuestaGrpc() {}

  public static <T> void responder(StreamObserver<T> observador, Supplier<T> operacion) {
    T respuesta;
    try {
      respuesta = operacion.get();
    } catch (ErrorNegocio error) {
      observador.onError(error.comoStatus().asRuntimeException());
      return;
    } catch (RuntimeException error) {
      log.error("Error no controlado en una llamada gRPC", error);
      observador.onError(Status.INTERNAL.withDescription("Error interno del servidor.").asRuntimeException());
      return;
    }
    observador.onNext(respuesta);
    observador.onCompleted();
  }
}
