package com.roomiesfind.ia.api;

import com.roomiesfind.comun.RespuestaGrpc;
import com.roomiesfind.contratos.ia.GenerarExplicacionRequest;
import com.roomiesfind.contratos.ia.GenerarExplicacionResponse;
import com.roomiesfind.contratos.ia.IaGrpc;
import com.roomiesfind.ia.controller.ExplicacionController;
import com.roomiesfind.ia.dto.SolicitudTextoDto;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

/**
 * API del microservicio: lo único que ven los demás módulos, y solo por gRPC.
 * Traduce entre los mensajes protobuf y los DTO de la capa interna.
 */
@Component
public class IaApi extends IaGrpc.IaImplBase {

  private final ExplicacionController controller;

  public IaApi(ExplicacionController controller) {
    this.controller = controller;
  }

  @Override
  public void generarExplicacion(GenerarExplicacionRequest request, StreamObserver<GenerarExplicacionResponse> observador) {
    RespuestaGrpc.responder(observador, () -> GenerarExplicacionResponse.newBuilder()
        .setTexto(controller.generar(new SolicitudTextoDto(
            request.getNombreUsuario(), request.getNombreCandidato(), request.getScore(),
            request.getCoincidenciasList(), request.getConflictosList())))
        .build());
  }
}
