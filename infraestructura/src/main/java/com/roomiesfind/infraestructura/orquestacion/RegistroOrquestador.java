package com.roomiesfind.infraestructura.orquestacion;

import com.roomiesfind.contratos.perfiles.CrearPerfilRequest;
import com.roomiesfind.contratos.perfiles.DatosPerfil;
import com.roomiesfind.contratos.perfiles.Foto;
import com.roomiesfind.contratos.perfiles.ValidarRegistroRequest;
import com.roomiesfind.contratos.usuarios.CrearUsuarioRequest;
import com.roomiesfind.contratos.usuarios.EliminarUsuarioRequest;
import com.roomiesfind.contratos.usuarios.SesionResponse;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Crear una cuenta toca dos microservicios, así que ya no hay una sola transacción:
 * primero se validan los datos del perfil y, si crear el perfil falla después de
 * crear el usuario, se elimina ese usuario. Nunca queda un usuario sin perfil.
 */
@Component
public class RegistroOrquestador {

  private static final Logger log = LoggerFactory.getLogger(RegistroOrquestador.class);

  private final ClientesGrpc clientes;

  public RegistroOrquestador(ClientesGrpc clientes) {
    this.clientes = clientes;
  }

  public SesionResponse registrar(String nombre, String email, String password, DatosPerfil datos, Foto foto) {
    ValidarRegistroRequest.Builder validacion = ValidarRegistroRequest.newBuilder().setDatos(datos);
    if (foto != null) validacion.setFoto(foto);
    clientes.perfiles().validarRegistro(validacion.build());

    SesionResponse sesion = clientes.usuarios().crearUsuario(CrearUsuarioRequest.newBuilder()
        .setNombre(nombre).setEmail(email).setPassword(password).build());

    int usuarioId = sesion.getUsuario().getId();
    try {
      CrearPerfilRequest.Builder perfil = CrearPerfilRequest.newBuilder().setUsuarioId(usuarioId).setDatos(datos);
      if (foto != null) perfil.setFoto(foto);
      clientes.perfiles().crearPerfil(perfil.build());
    } catch (RuntimeException error) {
      compensar(usuarioId);
      throw error;
    }
    return sesion;
  }

  private void compensar(int usuarioId) {
    try {
      clientes.usuarios().eliminarUsuario(EliminarUsuarioRequest.newBuilder().setId(usuarioId).build());
    } catch (RuntimeException error) {
      log.error("No se pudo deshacer el usuario {} tras fallar su perfil: {}", usuarioId, error.getMessage());
    }
  }
}
