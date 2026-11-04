package com.roomiesfind.infraestructura.orquestacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.Empty;
import com.roomiesfind.contratos.perfiles.CrearPerfilRequest;
import com.roomiesfind.contratos.perfiles.DatosPerfil;
import com.roomiesfind.contratos.perfiles.Perfil;
import com.roomiesfind.contratos.perfiles.PerfilesGrpc;
import com.roomiesfind.contratos.perfiles.ValidarRegistroRequest;
import com.roomiesfind.contratos.usuarios.CrearUsuarioRequest;
import com.roomiesfind.contratos.usuarios.EliminarUsuarioRequest;
import com.roomiesfind.contratos.usuarios.SesionResponse;
import com.roomiesfind.contratos.usuarios.Usuario;
import com.roomiesfind.contratos.usuarios.UsuariosGrpc;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Comprueba por gRPC real (en memoria) que el registro nunca deja un usuario sin perfil. */
class RegistroOrquestadorTest {

  private final List<String> llamados = new ArrayList<>();
  private Status falloValidacion;
  private Status falloPerfil;

  private Server servidor;
  private ManagedChannel canal;
  private RegistroOrquestador orquestador;

  @BeforeEach
  void iniciar() throws Exception {
    String nombre = InProcessServerBuilder.generateName();
    servidor = InProcessServerBuilder.forName(nombre).directExecutor()
        .addService(new UsuariosGrpc.UsuariosImplBase() {
          @Override
          public void crearUsuario(CrearUsuarioRequest request, StreamObserver<SesionResponse> observador) {
            llamados.add("crearUsuario");
            observador.onNext(SesionResponse.newBuilder().setToken("token")
                .setUsuario(Usuario.newBuilder().setId(7).setNombre(request.getNombre())).build());
            observador.onCompleted();
          }

          @Override
          public void eliminarUsuario(EliminarUsuarioRequest request, StreamObserver<Empty> observador) {
            llamados.add("eliminarUsuario:" + request.getId());
            observador.onNext(Empty.getDefaultInstance());
            observador.onCompleted();
          }
        })
        .addService(new PerfilesGrpc.PerfilesImplBase() {
          @Override
          public void validarRegistro(ValidarRegistroRequest request, StreamObserver<Empty> observador) {
            llamados.add("validarRegistro");
            if (falloValidacion != null) {
              observador.onError(falloValidacion.asRuntimeException());
              return;
            }
            observador.onNext(Empty.getDefaultInstance());
            observador.onCompleted();
          }

          @Override
          public void crearPerfil(CrearPerfilRequest request, StreamObserver<Perfil> observador) {
            llamados.add("crearPerfil:" + request.getUsuarioId());
            if (falloPerfil != null) {
              observador.onError(falloPerfil.asRuntimeException());
              return;
            }
            observador.onNext(Perfil.getDefaultInstance());
            observador.onCompleted();
          }
        })
        .build().start();
    canal = InProcessChannelBuilder.forName(nombre).directExecutor().build();
    orquestador = new RegistroOrquestador(new ClientesGrpc(canal, canal, canal));
  }

  @AfterEach
  void cerrar() {
    canal.shutdownNow();
    servidor.shutdownNow();
  }

  private SesionResponse registrar() {
    return orquestador.registrar("Ana", "ana@correo.co", "clave123", DatosPerfil.getDefaultInstance(), null);
  }

  @Test
  void creaElUsuarioYLuegoSuPerfil() {
    assertEquals("token", registrar().getToken());
    assertEquals(List.of("validarRegistro", "crearUsuario", "crearPerfil:7"), llamados);
  }

  @Test
  void siLosDatosSonInvalidosNoCreaElUsuario() {
    falloValidacion = Status.INVALID_ARGUMENT.withDescription("Elige un barrio de la lista.");
    StatusRuntimeException error = assertThrows(StatusRuntimeException.class, this::registrar);
    assertEquals("Elige un barrio de la lista.", error.getStatus().getDescription());
    assertEquals(List.of("validarRegistro"), llamados);
  }

  @Test
  void siFallaElPerfilEliminaElUsuarioCreado() {
    falloPerfil = Status.INTERNAL;
    assertThrows(StatusRuntimeException.class, this::registrar);
    assertEquals(List.of("validarRegistro", "crearUsuario", "crearPerfil:7", "eliminarUsuario:7"), llamados);
  }
}
