package com.roomiesfind.usuarios.controller;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.usuarios.dto.CredencialesDto;
import com.roomiesfind.usuarios.dto.RegistroUsuarioDto;
import com.roomiesfind.usuarios.dto.SesionDto;
import com.roomiesfind.usuarios.dto.UsuarioDto;
import com.roomiesfind.usuarios.service.TokenService;
import com.roomiesfind.usuarios.service.UsuarioService;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Valida la entrada y coordina los casos de uso. No conoce gRPC ni SQL. */
@Component
public class UsuarioController {

  private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  private final UsuarioService usuarios;
  private final TokenService tokens;

  public UsuarioController(UsuarioService usuarios, TokenService tokens) {
    this.usuarios = usuarios;
    this.tokens = tokens;
  }

  public SesionDto registrar(RegistroUsuarioDto datos) {
    validarCuenta(datos);
    UsuarioDto usuario = usuarios.registrar(datos);
    return new SesionDto(tokens.emitir(usuario.id()), usuario);
  }

  public SesionDto autenticar(CredencialesDto credenciales) {
    if (credenciales.email().isEmpty() || credenciales.password().isEmpty()) {
      throw ErrorNegocio.solicitudInvalida("email y password son obligatorios.");
    }
    UsuarioDto usuario = usuarios.autenticar(credenciales);
    return new SesionDto(tokens.emitir(usuario.id()), usuario);
  }

  public int validarToken(String token) {
    return tokens.validar(token);
  }

  public List<UsuarioDto> obtener(List<Integer> ids) {
    return usuarios.obtener(ids);
  }

  public List<UsuarioDto> administradores() {
    return usuarios.administradores();
  }

  public void eliminar(int id) {
    usuarios.eliminar(id);
  }

  static void validarCuenta(RegistroUsuarioDto datos) {
    if (datos.nombre().isBlank() || datos.nombre().length() > 120) {
      throw ErrorNegocio.solicitudInvalida("El nombre debe tener entre 1 y 120 caracteres.");
    }
    if (datos.email().length() > 160 || !CORREO.matcher(datos.email()).matches()) {
      throw ErrorNegocio.solicitudInvalida("El correo no es válido.");
    }
    if (datos.password().length() < 6) {
      throw ErrorNegocio.solicitudInvalida("La contraseña debe tener al menos 6 caracteres.");
    }
  }
}
