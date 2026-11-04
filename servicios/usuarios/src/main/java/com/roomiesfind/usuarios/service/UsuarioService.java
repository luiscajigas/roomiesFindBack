package com.roomiesfind.usuarios.service;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.usuarios.dto.CredencialesDto;
import com.roomiesfind.usuarios.dto.RegistroUsuarioDto;
import com.roomiesfind.usuarios.dto.UsuarioDto;
import com.roomiesfind.usuarios.model.Usuario;
import com.roomiesfind.usuarios.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/** Reglas de las cuentas. Las contraseñas solo existen aquí, siempre con bcrypt. */
@Service
public class UsuarioService {

  /** bcrypt solo usa los primeros 72 bytes; las cuentas existentes se crearon así. */
  private static final int BYTES_BCRYPT = 72;
  private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas.";

  private final UsuarioRepository repositorio;
  private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(10);

  public UsuarioService(UsuarioRepository repositorio) {
    this.repositorio = repositorio;
  }

  public UsuarioDto registrar(RegistroUsuarioDto datos) {
    String hash = bcrypt.encode(recortarParaBcrypt(datos.password()));
    try {
      return aDto(repositorio.crear(datos.nombre(), datos.email(), hash));
    } catch (DuplicateKeyException error) {
      throw ErrorNegocio.conflicto("Ya existe una cuenta con ese correo.");
    }
  }

  public UsuarioDto autenticar(CredencialesDto credenciales) {
    Usuario usuario = repositorio.buscarPorEmail(credenciales.email())
        .orElseThrow(() -> ErrorNegocio.noAutenticado(CREDENCIALES_INVALIDAS));
    if (!coincide(credenciales.password(), usuario.passwordHash())) {
      throw ErrorNegocio.noAutenticado(CREDENCIALES_INVALIDAS);
    }
    return aDto(usuario);
  }

  public List<UsuarioDto> obtener(List<Integer> ids) {
    return repositorio.buscarPorIds(ids).stream().map(UsuarioService::aDto).toList();
  }

  public void eliminar(int id) {
    repositorio.eliminar(id);
  }

  boolean coincide(String password, String hash) {
    return bcrypt.matches(recortarParaBcrypt(password), hash);
  }

  static String recortarParaBcrypt(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length <= BYTES_BCRYPT) return password;
    StringBuilder recortada = new StringBuilder();
    int bytes = 0;
    for (int i = 0; i < password.length(); ) {
      int caracter = password.codePointAt(i);
      String texto = new String(Character.toChars(caracter));
      bytes += texto.getBytes(StandardCharsets.UTF_8).length;
      if (bytes > BYTES_BCRYPT) break;
      recortada.append(texto);
      i += Character.charCount(caracter);
    }
    return recortada.toString();
  }

  private static UsuarioDto aDto(Usuario usuario) {
    return new UsuarioDto(usuario.id(), usuario.nombre(), usuario.email());
  }
}
