package com.roomiesfind.moderacion.api;

import com.google.protobuf.Empty;
import com.roomiesfind.comun.RespuestaGrpc;
import com.roomiesfind.contratos.moderacion.CuentaReportada;
import com.roomiesfind.contratos.moderacion.CuentasRequest;
import com.roomiesfind.contratos.moderacion.EstaSuspendidaResponse;
import com.roomiesfind.contratos.moderacion.ListarReportesRequest;
import com.roomiesfind.contratos.moderacion.ListarReportesResponse;
import com.roomiesfind.contratos.moderacion.ListarSuspendidasResponse;
import com.roomiesfind.contratos.moderacion.EstanBloqueadosResponse;
import com.roomiesfind.contratos.moderacion.IdsResponse;
import com.roomiesfind.contratos.moderacion.ModeracionGrpc;
import com.roomiesfind.contratos.moderacion.ParejaRequest;
import com.roomiesfind.contratos.moderacion.ReportarRequest;
import com.roomiesfind.contratos.moderacion.Reporte;
import com.roomiesfind.contratos.moderacion.ReportarResponse;
import com.roomiesfind.contratos.moderacion.ResolverReportesRequest;
import com.roomiesfind.contratos.moderacion.ResumenModeracion;
import com.roomiesfind.contratos.moderacion.SuspenderRequest;
import com.roomiesfind.contratos.moderacion.Suspension;
import com.roomiesfind.contratos.moderacion.UsuarioRequest;
import com.roomiesfind.moderacion.controller.ModeracionController;
import com.roomiesfind.moderacion.dto.NuevoReporteDto;
import com.roomiesfind.moderacion.dto.ResumenDto;
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

  @Override
  public void obtenerResumen(Empty request, StreamObserver<ResumenModeracion> observador) {
    RespuestaGrpc.responder(observador, () -> {
      ResumenDto resumen = controller.resumen();
      return ResumenModeracion.newBuilder()
          .setCuentasPorRevisar(resumen.cuentasPorRevisar())
          .setReportesPendientes(resumen.reportesPendientes())
          .setAlertasActivas(resumen.alertasActivas())
          .setCuentasSuspendidas(resumen.cuentasSuspendidas())
          .build();
    });
  }

  @Override
  public void listarReportes(ListarReportesRequest request, StreamObserver<ListarReportesResponse> observador) {
    RespuestaGrpc.responder(observador, () -> ListarReportesResponse.newBuilder()
        .addAllCuentas(controller.cuentasReportadas(request.getSoloPendientes()).stream()
            .map((cuenta) -> CuentaReportada.newBuilder()
                .setUsuarioId(cuenta.usuarioId())
                .setAlerta(cuenta.alerta())
                .setSuspendida(cuenta.suspendida())
                .addAllReportes(cuenta.reportes().stream().map(ModeracionApi::aReporte).toList())
                .build())
            .toList())
        .build());
  }

  @Override
  public void resolverReportes(ResolverReportesRequest request, StreamObserver<Empty> observador) {
    RespuestaGrpc.responder(observador, () -> {
      controller.resolver(request.getAdminId(), request.getUsuarioId(), request.getDecision());
      return Empty.getDefaultInstance();
    });
  }

  @Override
  public void suspender(SuspenderRequest request, StreamObserver<Empty> observador) {
    RespuestaGrpc.responder(observador, () -> {
      controller.suspender(request.getAdminId(), request.getUsuarioId(), request.getMotivo());
      return Empty.getDefaultInstance();
    });
  }

  @Override
  public void reactivar(UsuarioRequest request, StreamObserver<Empty> observador) {
    RespuestaGrpc.responder(observador, () -> {
      controller.reactivar(request.getUsuarioId());
      return Empty.getDefaultInstance();
    });
  }

  @Override
  public void listarSuspendidas(Empty request, StreamObserver<ListarSuspendidasResponse> observador) {
    RespuestaGrpc.responder(observador, () -> ListarSuspendidasResponse.newBuilder()
        .addAllSuspensiones(controller.suspendidas().stream()
            .map((suspension) -> {
              Suspension.Builder fila = Suspension.newBuilder()
                  .setUsuarioId(suspension.usuarioId())
                  .setMotivo(suspension.motivo())
                  .setCreadaEn(suspension.creadaEn());
              if (suspension.suspendidaPor() != null) fila.setSuspendidaPor(suspension.suspendidaPor());
              return fila.build();
            })
            .toList())
        .build());
  }

  @Override
  public void estaSuspendida(UsuarioRequest request, StreamObserver<EstaSuspendidaResponse> observador) {
    RespuestaGrpc.responder(observador, () -> EstaSuspendidaResponse.newBuilder()
        .setSuspendida(controller.estaSuspendida(request.getUsuarioId()))
        .build());
  }

  private static Reporte aReporte(com.roomiesfind.moderacion.model.Reporte reporte) {
    Reporte.Builder fila = Reporte.newBuilder()
        .setId(reporte.id())
        .setReportanteId(reporte.reportanteId())
        .setMotivo(reporte.motivo())
        .setEstado(reporte.estado())
        .setCreadoEn(reporte.creadoEn());
    if (reporte.detalle() != null) fila.setDetalle(reporte.detalle());
    return fila.build();
  }

  private static IdsResponse aIds(List<Integer> ids) {
    return IdsResponse.newBuilder().addAllIds(ids).build();
  }
}
