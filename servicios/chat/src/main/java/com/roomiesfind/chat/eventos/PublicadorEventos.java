package com.roomiesfind.chat.eventos;

import com.roomiesfind.contratos.chat.EventoChat;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos en Redis. Todo pasa por aquí, incluso si el destinatario
 * está conectado a esta misma instancia: así funciona igual con varias copias del servicio.
 */
@Component
public class PublicadorEventos {

  private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);

  private final RedisConnectionFactory conexiones;

  public PublicadorEventos(RedisConnectionFactory conexiones) {
    this.conexiones = conexiones;
  }

  /** Si Redis falla, el mensaje ya quedó guardado: solo se pierde el aviso en vivo. */
  public void publicar(int usuarioId, EventoChat evento) {
    byte[] canal = SuscriptoresEventos.canalDe(usuarioId).getBytes(StandardCharsets.UTF_8);
    try (RedisConnection conexion = conexiones.getConnection()) {
      conexion.publish(canal, evento.toByteArray());
    } catch (RuntimeException error) {
      log.warn("No se pudo publicar un evento en Redis: {}", error.getMessage());
    }
  }
}
