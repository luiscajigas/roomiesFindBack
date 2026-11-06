package com.roomiesfind.chat.eventos;

import com.roomiesfind.contratos.chat.EventoChat;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos en Redis. Todo pasa por aquí, incluso si el destinatario
 * está conectado a esta misma instancia: así funciona igual con varias copias del servicio.
 *
 * <p>Publica en un solo hilo aparte: conserva el orden y, si Redis se cae, enviar
 * un mensaje sigue siendo inmediato. Solo se pierde el aviso en vivo, porque el
 * mensaje ya quedó guardado. Si la cola se llena, se descartan los avisos más viejos.
 */
@Component
public class PublicadorEventos {

  private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);
  private static final int AVISOS_EN_COLA = 1000;

  private final RedisConnectionFactory conexiones;
  private final ThreadPoolExecutor hilo = new ThreadPoolExecutor(
      1, 1, 0, TimeUnit.MILLISECONDS,
      new ArrayBlockingQueue<>(AVISOS_EN_COLA),
      (tarea) -> {
        Thread publicador = new Thread(tarea, "chat-publicador");
        publicador.setDaemon(true);
        return publicador;
      },
      new ThreadPoolExecutor.DiscardOldestPolicy());

  public PublicadorEventos(RedisConnectionFactory conexiones) {
    this.conexiones = conexiones;
  }

  public void publicar(int usuarioId, EventoChat evento) {
    hilo.execute(() -> enviarARedis(usuarioId, evento));
  }

  @PreDestroy
  void cerrar() {
    hilo.shutdown();
  }

  private void enviarARedis(int usuarioId, EventoChat evento) {
    byte[] canal = SuscriptoresEventos.canalDe(usuarioId).getBytes(StandardCharsets.UTF_8);
    try (RedisConnection conexion = conexiones.getConnection()) {
      conexion.publish(canal, evento.toByteArray());
    } catch (RuntimeException error) {
      log.warn("No se pudo publicar un evento en Redis: {}", error.getMessage());
    }
  }
}
