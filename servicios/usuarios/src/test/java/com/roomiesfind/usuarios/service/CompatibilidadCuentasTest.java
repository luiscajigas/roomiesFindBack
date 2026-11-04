package com.roomiesfind.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.roomiesfind.comun.ErrorNegocio;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Las cuentas y sesiones creadas por el backend en Node deben seguir sirviendo. */
class CompatibilidadCuentasTest {

  /** Generado con bcryptjs: bcrypt.hash('clave123', 10). */
  private static final String HASH_DE_NODE = "$2b$10$gb.ZooJtx3wMSrDk3kSMCuNxANqJLM9M1MfEsks.P.kaf.U52jg0.";

  @Test
  void aceptaContrasenasGuardadasPorElBackendAnterior() {
    UsuarioService servicio = new UsuarioService(null);
    assertTrue(servicio.coincide("clave123", HASH_DE_NODE));
    assertFalse(servicio.coincide("otra-clave", HASH_DE_NODE));
  }

  @Test
  void recortaComoBcryptSinPartirCaracteres() {
    assertEquals("a".repeat(72), UsuarioService.recortarParaBcrypt("a".repeat(100)));
    assertEquals("ñ".repeat(36), UsuarioService.recortarParaBcrypt("ñ".repeat(50)));
    assertEquals("corta", UsuarioService.recortarParaBcrypt("corta"));
  }

  @Test
  void emiteYValidaSesiones() {
    TokenService tokens = new TokenService("secreto");
    assertEquals(42, tokens.validar(tokens.emitir(42)));
  }

  @Test
  void aceptaTokensDelBackendAnterior() {
    String token = JWT.create().withClaim("usuarioId", 7).withExpiresAt(Instant.now().plusSeconds(60))
        .sign(Algorithm.HMAC256("secreto"));
    assertEquals(7, new TokenService("secreto").validar(token));
  }

  @Test
  void rechazaTokensVencidosOFirmadosConOtroSecreto() {
    TokenService tokens = new TokenService("secreto");
    String vencido = JWT.create().withClaim("usuarioId", 7).withExpiresAt(Instant.now().minusSeconds(60))
        .sign(Algorithm.HMAC256("secreto"));
    String ajeno = new TokenService("otro").emitir(7);

    assertThrows(ErrorNegocio.class, () -> tokens.validar(vencido));
    assertThrows(ErrorNegocio.class, () -> tokens.validar(ajeno));
    assertThrows(ErrorNegocio.class, () -> tokens.validar("basura"));
  }
}
