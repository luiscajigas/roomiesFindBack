package com.roomiesfind.infraestructura.contrato;

import com.roomiesfind.contratos.usuarios.SesionResponse;
import com.roomiesfind.contratos.usuarios.Usuario;

/** Respuestas de registro, inicio de sesión y sesión actual. */
public final class AuthRespuestas {

  private AuthRespuestas() {}

  /** @param rol "usuario" o "admin" */
  public record UsuarioSesion(int id, String nombre, String email, String rol) {

    public static UsuarioSesion de(Usuario usuario) {
      return new UsuarioSesion(usuario.getId(), usuario.getNombre(), usuario.getEmail(), Roles.normalizar(usuario.getRol()));
    }
  }

  public record SesionRespuesta(String token, UsuarioSesion usuario) {

    public static SesionRespuesta de(SesionResponse sesion) {
      return new SesionRespuesta(sesion.getToken(), UsuarioSesion.de(sesion.getUsuario()));
    }
  }
}
