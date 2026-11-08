package com.roomiesfind.infraestructura.seguridad;

import com.roomiesfind.contratos.usuarios.ObtenerUsuariosRequest;
import com.roomiesfind.contratos.usuarios.ValidarTokenRequest;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.rest.ErrorRest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Pide al microservicio de usuarios que valide el token de cada ruta protegida, rechaza
 * las cuentas suspendidas y, en las rutas de administración, exige el rol de administrador.
 */
@Component
public class AutenticacionInterceptor implements HandlerInterceptor {

  public static final String ATRIBUTO_USUARIO = "usuarioId";
  public static final String CUENTA_SUSPENDIDA = "Tu cuenta fue suspendida.";
  private static final String PREFIJO_BEARER = "Bearer ";

  private final ClientesGrpc clientes;
  private final Suspensiones suspensiones;

  public AutenticacionInterceptor(ClientesGrpc clientes, Suspensiones suspensiones) {
    this.clientes = clientes;
    this.suspensiones = suspensiones;
  }

  @Override
  public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
    if (!(manejador instanceof HandlerMethod metodo)) return true;
    boolean soloAdmin = metodo.hasMethodAnnotation(RequiereAdmin.class);
    if (!soloAdmin && !metodo.hasMethodAnnotation(RequiereAuth.class)) return true;

    String encabezado = peticion.getHeader("Authorization");
    if (encabezado == null || !encabezado.startsWith(PREFIJO_BEARER)) {
      throw new ErrorRest(HttpStatus.UNAUTHORIZED, "No autenticado. Falta el token.");
    }
    int usuarioId = clientes.usuarios()
        .validarToken(ValidarTokenRequest.newBuilder().setToken(encabezado.substring(PREFIJO_BEARER.length())).build())
        .getUsuarioId();
    if (suspensiones.estaSuspendida(usuarioId)) {
      throw new ErrorRest(HttpStatus.FORBIDDEN, CUENTA_SUSPENDIDA);
    }
    if (soloAdmin && !esAdmin(usuarioId)) {
      throw new ErrorRest(HttpStatus.FORBIDDEN, "No tienes permiso para entrar aquí.");
    }
    peticion.setAttribute(ATRIBUTO_USUARIO, usuarioId);
    return true;
  }

  /** El rol se lee en cada petición: si se lo quitan a alguien, deja de ser administrador al instante. */
  private boolean esAdmin(int usuarioId) {
    return clientes.usuarios()
        .obtenerUsuarios(ObtenerUsuariosRequest.newBuilder().addIds(usuarioId).build())
        .getUsuariosList().stream()
        .anyMatch((usuario) -> "admin".equals(usuario.getRol()));
  }
}
