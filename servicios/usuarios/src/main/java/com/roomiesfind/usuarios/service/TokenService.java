package com.roomiesfind.usuarios.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.roomiesfind.comun.Entorno;
import com.roomiesfind.comun.ErrorNegocio;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** Emite y valida las sesiones. Mismo formato que el backend anterior: las sesiones abiertas siguen sirviendo. */
@Service
public class TokenService {

  private static final Duration DURACION = Duration.ofDays(7);
  private static final String SECRETO_DESARROLLO = "secreto-local-de-desarrollo";
  private static final String CAMPO_USUARIO = "usuarioId";

  private final Algorithm algoritmo;

  public TokenService() {
    this(Entorno.esProduccion()
        ? Entorno.obligatoria("JWT_SECRET")
        : Entorno.opcional("JWT_SECRET", SECRETO_DESARROLLO));
  }

  TokenService(String secreto) {
    this.algoritmo = Algorithm.HMAC256(secreto);
  }

  public String emitir(int usuarioId) {
    Instant ahora = Instant.now();
    return JWT.create()
        .withClaim(CAMPO_USUARIO, usuarioId)
        .withIssuedAt(ahora)
        .withExpiresAt(ahora.plus(DURACION))
        .sign(algoritmo);
  }

  public int validar(String token) {
    try {
      DecodedJWT decodificado = JWT.require(algoritmo).build().verify(token);
      Integer usuarioId = decodificado.getClaim(CAMPO_USUARIO).asInt();
      if (usuarioId == null) throw new JWTVerificationException("Sin usuario");
      return usuarioId;
    } catch (JWTVerificationException error) {
      throw ErrorNegocio.noAutenticado("Token inválido o expirado.");
    }
  }
}
