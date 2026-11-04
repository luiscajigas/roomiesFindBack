package com.roomiesfind.infraestructura.seguridad;

import com.roomiesfind.contratos.usuarios.ValidarTokenRequest;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.rest.ErrorRest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Pide al microservicio de usuarios que valide el token de cada ruta protegida. */
@Component
public class AutenticacionInterceptor implements HandlerInterceptor {

  public static final String ATRIBUTO_USUARIO = "usuarioId";
  private static final String PREFIJO_BEARER = "Bearer ";

  private final ClientesGrpc clientes;

  public AutenticacionInterceptor(ClientesGrpc clientes) {
    this.clientes = clientes;
  }

  @Override
  public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
    if (!(manejador instanceof HandlerMethod metodo) || !metodo.hasMethodAnnotation(RequiereAuth.class)) {
      return true;
    }
    String encabezado = peticion.getHeader("Authorization");
    if (encabezado == null || !encabezado.startsWith(PREFIJO_BEARER)) {
      throw new ErrorRest(HttpStatus.UNAUTHORIZED, "No autenticado. Falta el token.");
    }
    int usuarioId = clientes.usuarios()
        .validarToken(ValidarTokenRequest.newBuilder().setToken(encabezado.substring(PREFIJO_BEARER.length())).build())
        .getUsuarioId();
    peticion.setAttribute(ATRIBUTO_USUARIO, usuarioId);
    return true;
  }
}
