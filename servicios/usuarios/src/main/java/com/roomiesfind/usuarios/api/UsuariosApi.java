package com.roomiesfind.usuarios.api;

import com.google.protobuf.Empty;
import com.roomiesfind.comun.RespuestaGrpc;
import com.roomiesfind.contratos.usuarios.AutenticarRequest;
import com.roomiesfind.contratos.usuarios.CrearUsuarioRequest;
import com.roomiesfind.contratos.usuarios.EliminarUsuarioRequest;
import com.roomiesfind.contratos.usuarios.ObtenerUsuariosRequest;
import com.roomiesfind.contratos.usuarios.ObtenerUsuariosResponse;
import com.roomiesfind.contratos.usuarios.SesionResponse;
import com.roomiesfind.contratos.usuarios.Usuario;
import com.roomiesfind.contratos.usuarios.UsuariosGrpc;
import com.roomiesfind.contratos.usuarios.ValidarTokenRequest;
import com.roomiesfind.contratos.usuarios.ValidarTokenResponse;
import com.roomiesfind.usuarios.controller.UsuarioController;
import com.roomiesfind.usuarios.dto.CredencialesDto;
import com.roomiesfind.usuarios.dto.RegistroUsuarioDto;
import com.roomiesfind.usuarios.dto.SesionDto;
import com.roomiesfind.usuarios.dto.UsuarioDto;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

/**
 * API del microservicio: lo único que ven los demás módulos, y solo por gRPC.
 * Traduce entre los mensajes protobuf y los DTO de la capa interna.
 */
@Component
public class UsuariosApi extends UsuariosGrpc.UsuariosImplBase {

  private final UsuarioController controller;

  public UsuariosApi(UsuarioController controller) {
    this.controller = controller;
  }

  @Override
  public void crearUsuario(CrearUsuarioRequest request, StreamObserver<SesionResponse> observador) {
    RespuestaGrpc.responder(observador, () -> aSesion(controller.registrar(
        new RegistroUsuarioDto(request.getNombre(), request.getEmail(), request.getPassword()))));
  }

  @Override
  public void autenticar(AutenticarRequest request, StreamObserver<SesionResponse> observador) {
    RespuestaGrpc.responder(observador, () -> aSesion(controller.autenticar(
        new CredencialesDto(request.getEmail(), request.getPassword()))));
  }

  @Override
  public void validarToken(ValidarTokenRequest request, StreamObserver<ValidarTokenResponse> observador) {
    RespuestaGrpc.responder(observador, () -> ValidarTokenResponse.newBuilder()
        .setUsuarioId(controller.validarToken(request.getToken()))
        .build());
  }

  @Override
  public void obtenerUsuarios(ObtenerUsuariosRequest request, StreamObserver<ObtenerUsuariosResponse> observador) {
    RespuestaGrpc.responder(observador, () -> ObtenerUsuariosResponse.newBuilder()
        .addAllUsuarios(controller.obtener(request.getIdsList()).stream().map(UsuariosApi::aUsuario).toList())
        .build());
  }

  @Override
  public void eliminarUsuario(EliminarUsuarioRequest request, StreamObserver<Empty> observador) {
    RespuestaGrpc.responder(observador, () -> {
      controller.eliminar(request.getId());
      return Empty.getDefaultInstance();
    });
  }

  private static SesionResponse aSesion(SesionDto sesion) {
    return SesionResponse.newBuilder().setToken(sesion.token()).setUsuario(aUsuario(sesion.usuario())).build();
  }

  private static Usuario aUsuario(UsuarioDto usuario) {
    return Usuario.newBuilder().setId(usuario.id()).setNombre(usuario.nombre()).setEmail(usuario.email()).build();
  }
}
