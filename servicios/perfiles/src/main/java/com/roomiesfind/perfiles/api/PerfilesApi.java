package com.roomiesfind.perfiles.api;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.roomiesfind.comun.RespuestaGrpc;
import com.roomiesfind.contratos.perfiles.ActualizarPerfilRequest;
import com.roomiesfind.contratos.perfiles.CrearPerfilRequest;
import com.roomiesfind.contratos.perfiles.DatosPerfil;
import com.roomiesfind.contratos.perfiles.Foto;
import com.roomiesfind.contratos.perfiles.GuardarFotoRequest;
import com.roomiesfind.contratos.perfiles.GuardarFotoResponse;
import com.roomiesfind.contratos.perfiles.ListarCandidatosRequest;
import com.roomiesfind.contratos.perfiles.ListarCandidatosResponse;
import com.roomiesfind.contratos.perfiles.ObtenerFotoRequest;
import com.roomiesfind.contratos.perfiles.ObtenerPerfilRequest;
import com.roomiesfind.contratos.perfiles.ObtenerPerfilesRequest;
import com.roomiesfind.contratos.perfiles.ObtenerPerfilesResponse;
import com.roomiesfind.contratos.perfiles.Perfil;
import com.roomiesfind.contratos.perfiles.PerfilesGrpc;
import com.roomiesfind.contratos.perfiles.ValidarRegistroRequest;
import com.roomiesfind.perfiles.controller.PerfilController;
import com.roomiesfind.perfiles.dto.DatosPerfilDto;
import com.roomiesfind.perfiles.dto.FotoDto;
import com.roomiesfind.perfiles.dto.PerfilDto;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

/**
 * API del microservicio: lo único que ven los demás módulos, y solo por gRPC.
 * Traduce entre los mensajes protobuf y los DTO de la capa interna.
 */
@Component
public class PerfilesApi extends PerfilesGrpc.PerfilesImplBase {

  private final PerfilController controller;

  public PerfilesApi(PerfilController controller) {
    this.controller = controller;
  }

  @Override
  public void validarRegistro(ValidarRegistroRequest request, StreamObserver<Empty> observador) {
    RespuestaGrpc.responder(observador, () -> {
      controller.validarRegistro(aDatos(request.getDatos()), request.hasFoto() ? aFoto(request.getFoto()) : null);
      return Empty.getDefaultInstance();
    });
  }

  @Override
  public void crearPerfil(CrearPerfilRequest request, StreamObserver<Perfil> observador) {
    RespuestaGrpc.responder(observador, () -> aPerfil(controller.crear(
        request.getUsuarioId(), aDatos(request.getDatos()), request.hasFoto() ? aFoto(request.getFoto()) : null)));
  }

  @Override
  public void obtenerPerfil(ObtenerPerfilRequest request, StreamObserver<Perfil> observador) {
    RespuestaGrpc.responder(observador, () -> aPerfil(controller.obtener(request.getUsuarioId())));
  }

  @Override
  public void obtenerPerfiles(ObtenerPerfilesRequest request, StreamObserver<ObtenerPerfilesResponse> observador) {
    RespuestaGrpc.responder(observador, () -> ObtenerPerfilesResponse.newBuilder()
        .addAllPerfiles(controller.obtenerVarios(request.getUsuarioIdsList()).stream().map(PerfilesApi::aPerfil).toList())
        .build());
  }

  @Override
  public void actualizarPerfil(ActualizarPerfilRequest request, StreamObserver<Perfil> observador) {
    RespuestaGrpc.responder(observador, () -> aPerfil(controller.actualizar(request.getUsuarioId(), aDatos(request.getDatos()))));
  }

  @Override
  public void listarCandidatos(ListarCandidatosRequest request, StreamObserver<ListarCandidatosResponse> observador) {
    RespuestaGrpc.responder(observador, () -> ListarCandidatosResponse.newBuilder()
        .addAllPerfiles(controller.candidatos(request.getExcluirUsuarioId()).stream().map(PerfilesApi::aPerfil).toList())
        .build());
  }

  @Override
  public void obtenerFoto(ObtenerFotoRequest request, StreamObserver<Foto> observador) {
    RespuestaGrpc.responder(observador, () -> {
      FotoDto foto = controller.obtenerFoto(request.getUsuarioId());
      return Foto.newBuilder().setTipoMime(foto.tipoMime()).setContenido(ByteString.copyFrom(foto.contenido())).build();
    });
  }

  @Override
  public void guardarFoto(GuardarFotoRequest request, StreamObserver<GuardarFotoResponse> observador) {
    RespuestaGrpc.responder(observador, () -> GuardarFotoResponse.newBuilder()
        .setFotoUrl(controller.guardarFoto(request.getUsuarioId(), aFoto(request.getFoto())))
        .build());
  }

  private static DatosPerfilDto aDatos(DatosPerfil datos) {
    return new DatosPerfilDto(
        datos.hasPresupuesto() ? datos.getPresupuesto() : null,
        datos.hasZona() ? datos.getZona() : null,
        datos.hasHorario() ? datos.getHorario() : null,
        datos.hasLimpieza() ? datos.getLimpieza() : null,
        datos.hasToleranciaRuido() ? datos.getToleranciaRuido() : null,
        datos.hasFrecuenciaVisitas() ? datos.getFrecuenciaVisitas() : null,
        datos.hasTieneMascotas() ? datos.getTieneMascotas() : null,
        datos.hasAceptaMascotas() ? datos.getAceptaMascotas() : null,
        datos.hasDescripcion() ? datos.getDescripcion() : null,
        datos.hasFotoUrl() ? datos.getFotoUrl() : null);
  }

  private static FotoDto aFoto(Foto foto) {
    return new FotoDto(foto.getTipoMime(), foto.getContenido().toByteArray());
  }

  private static Perfil aPerfil(PerfilDto perfil) {
    Perfil.Builder respuesta = Perfil.newBuilder()
        .setUsuarioId(perfil.usuarioId())
        .setPresupuesto(perfil.presupuesto())
        .setZonaId(perfil.zonaId())
        .setZona(perfil.zona())
        .setHorario(perfil.horario())
        .setLimpieza(perfil.limpieza())
        .setToleranciaRuido(perfil.toleranciaRuido())
        .setFrecuenciaVisitas(perfil.frecuenciaVisitas())
        .setTieneMascotas(perfil.tieneMascotas())
        .setAceptaMascotas(perfil.aceptaMascotas())
        .setActualizadoEn(perfil.actualizadoEn());
    if (perfil.descripcion() != null) respuesta.setDescripcion(perfil.descripcion());
    if (perfil.fotoUrl() != null) respuesta.setFotoUrl(perfil.fotoUrl());
    return respuesta.build();
  }
}
