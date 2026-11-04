package com.roomiesfind.infraestructura.rest;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Convierte los estados gRPC de los microservicios en respuestas HTTP para el frontend. */
@RestControllerAdvice
public class ManejadorErrores {

  private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);
  private static final String SERVICIO_NO_DISPONIBLE = "Un servicio no está disponible. Inténtalo de nuevo en unos segundos.";

  @ExceptionHandler(StatusRuntimeException.class)
  ResponseEntity<Map<String, String>> errorGrpc(StatusRuntimeException error) {
    Status status = error.getStatus();
    String mensaje = status.getDescription();
    return switch (status.getCode()) {
      case INVALID_ARGUMENT -> respuesta(HttpStatus.BAD_REQUEST, mensaje);
      case UNAUTHENTICATED -> respuesta(HttpStatus.UNAUTHORIZED, mensaje);
      case NOT_FOUND -> respuesta(HttpStatus.NOT_FOUND, mensaje);
      case ALREADY_EXISTS -> respuesta(HttpStatus.CONFLICT, mensaje);
      case FAILED_PRECONDITION -> respuesta(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
      case UNAVAILABLE, DEADLINE_EXCEEDED -> {
        log.error("Un microservicio no respondió: {}", status);
        yield respuesta(HttpStatus.SERVICE_UNAVAILABLE, SERVICIO_NO_DISPONIBLE);
      }
      default -> {
        log.error("Error en un microservicio: {}", status);
        yield respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.");
      }
    };
  }

  @ExceptionHandler(ErrorRest.class)
  ResponseEntity<Map<String, String>> errorRest(ErrorRest error) {
    return respuesta(error.status(), error.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<Map<String, String>> fotoMuyGrande(MaxUploadSizeExceededException error) {
    return respuesta(HttpStatus.BAD_REQUEST, "La foto debe pesar 5 MB o menos.");
  }

  @ExceptionHandler(MultipartException.class)
  ResponseEntity<Map<String, String>> fotoInvalida(MultipartException error) {
    return respuesta(HttpStatus.BAD_REQUEST, "No se pudo recibir la foto.");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Map<String, String>> cuerpoInvalido(HttpMessageNotReadableException error) {
    return respuesta(HttpStatus.BAD_REQUEST, "La solicitud no es válida.");
  }

  @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
  ResponseEntity<Map<String, String>> rutaNoEncontrada(Exception error) {
    return respuesta(HttpStatus.NOT_FOUND, "Ruta no encontrada.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, String>> errorInesperado(Exception error) {
    log.error("Error no controlado", error);
    return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.");
  }

  private static ResponseEntity<Map<String, String>> respuesta(HttpStatus status, String mensaje) {
    return ResponseEntity.status(status).body(Map.of("error", mensaje));
  }
}
