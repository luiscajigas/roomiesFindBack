package com.roomiesfind.moderacion.api;

import com.google.protobuf.Empty;
import com.roomiesfind.comun.RespuestaGrpc;
import com.roomiesfind.contratos.moderacion.CuentasRequest;
import com.roomiesfind.contratos.moderacion.EstanBloqueadosResponse;
import com.roomiesfind.contratos.moderacion.IdsResponse;
import com.roomiesfind.contratos.moderacion.ModeracionGrpc;
import com.roomiesfind.contratos.moderacion.ParejaRequest;
import com.roomiesfind.contratos.moderacion.ReportarRequest;
import com.roomiesfind.contratos.moderacion.ReportarResponse;
import com.roomiesfind.contratos.moderacion.UsuarioRequest;
import com.roomiesfind.moderacion.controller.ModeracionController;
import com.roomiesfind.moderacion.dto.NuevoReporteDto;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * API del microservicio: lo único que ven los demás módulos, y solo por gRPC.
 * Traduce entre los mensajes protobuf y los DTO de la capa interna.
 */
@Component
public class ModeracionApi extends ModeracionGrpc.ModeracionImplBase {

  private final ModeracionController controller;

  public ModeracionApi(ModeracionController controller) {
    this.controller = controller;
  }

  @Override
  public void bloquear(ParejaRequest request, StreamObserver<Empty> observador) {
    RespuestaGrpc.responder(observador, () -> {
      controller.bloquear(request.getUsuarioId(), request.getOtroId());
      return Empty.getDefaultInstance();
    });
  }

  @Override
  public void desbloquear(ParejaRequest request, StreamObserver<Empty> observador) {
    RespuestaGrpc.responder(observador, () -> {
      controller.desbloquear(request.getUsuarioId(), request.getOtroId());
      return Empty.getDefaultInstance();
    });
  }

  @Override
  public void listarBloqueados(UsuarioRequest request, StreamObserver<IdsResponse> observador) {
    RespuestaGrpc.responder(observador, () -> aIds(controller.bloqueados(request.getUsuarioId())));
  }

  @Override
  public void listarExcluidos(UsuarioRequest request, StreamObserver<IdsResponse> observador) {
    RespuestaGrpc.responder(observador, () -> aIds(controller.excluidos(request.getUsuarioId())));
  }

  @Override
  public void estanBloqueados(ParejaRequest request, StreamObserver<EstanBloqueadosResponse> observador) {
    RespuestaGrpc.responder(observador, () -> EstanBloqueadosResponse.newBuilder()
        .setBloqueados(controller.estanBloqueados(request.getUsuarioId(), request.getOtroId()))
        .build());
  }

  @Override
  public void reportar(ReportarRequest request, StreamObserver<ReportarResponse> observador) {
    RespuestaGrpc.responder(observador, () -> ReportarResponse.newBuilder()
        .setAlertaActivada(controller.reportar(new NuevoReporteDto(
            request.getReportanteId(), request.getReportadoId(), request.getMotivo(), request.getDetalle())))
        .build());
  }

  @Override
  public void cuentasConAlerta(CuentasRequest request, StreamObserver<IdsResponse> observador) {
    RespuestaGrpc.responder(observador, () -> aIds(controller.cuentasConAlerta(request.getIdsList())));
  }

  private static IdsResponse aIds(List<Integer> ids) {
    return IdsResponse.newBuilder().addAllIds(ids).build();
  }
}
