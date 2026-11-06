package com.roomiesfind.compatibilidad.api;

import com.google.protobuf.Empty;
import com.roomiesfind.comun.RespuestaGrpc;
import com.roomiesfind.compatibilidad.controller.CompatibilidadController;
import com.roomiesfind.compatibilidad.dto.ExplicacionDto;
import com.roomiesfind.compatibilidad.dto.SolicitudExplicacionDto;
import com.roomiesfind.contratos.compatibilidad.CompatibilidadGrpc;
import com.roomiesfind.contratos.compatibilidad.ExplicarRequest;
import com.roomiesfind.contratos.compatibilidad.ExplicarResponse;
import com.roomiesfind.contratos.compatibilidad.RegistrarRecomendacionRequest;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

/**
 * API del microservicio: lo único que ven los demás módulos, y solo por gRPC.
 * Traduce entre los mensajes protobuf y los DTO de la capa interna.
 */
@Component
public class CompatibilidadApi extends CompatibilidadGrpc.CompatibilidadImplBase {

  private final CompatibilidadController controller;

  public CompatibilidadApi(CompatibilidadController controller) {
    this.controller = controller;
  }

  @Override
  public void registrarRecomendacion(RegistrarRecomendacionRequest request, StreamObserver<Empty> observador) {
    RespuestaGrpc.responder(observador, () -> {
      controller.registrar(
          request.getUsuarioId(),
          request.hasCandidatoId() ? request.getCandidatoId() : null,
          request.hasScore() ? request.getScore() : null);
      return Empty.getDefaultInstance();
    });
  }

  @Override
  public void explicar(ExplicarRequest request, StreamObserver<ExplicarResponse> observador) {
    RespuestaGrpc.responder(observador, () -> {
      ExplicacionDto explicacion = controller.explicar(new SolicitudExplicacionDto(
          request.getUsuarioId(), request.getCandidatoId(), request.getScore(),
          request.getCoincidenciasList(), request.getConflictosList(), request.getIdioma()));
      return ExplicarResponse.newBuilder()
          .setId(explicacion.id())
          .setEstado(explicacion.estado())
          .setExplicacion(explicacion.explicacion())
          .setScore(explicacion.score())
          .setDesdeCache(explicacion.desdeCache())
          .build();
    });
  }
}
