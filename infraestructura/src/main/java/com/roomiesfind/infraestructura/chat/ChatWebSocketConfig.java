package com.roomiesfind.infraestructura.chat;

import com.roomiesfind.comun.Entorno;
import com.roomiesfind.infraestructura.config.WebConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/** Publica el WebSocket del chat y solo acepta los mismos orígenes que la API. */
@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {

  /** Alcanza para un mensaje de 2000 caracteres con su JSON, aunque cada uno ocupe varios bytes. */
  private static final int TAMANO_MAXIMO_MENSAJE = 16 * 1024;
  /** El navegador envía ping cada 30 segundos; una conexión muda más tiempo ya está muerta. */
  private static final long ESPERA_MAXIMA_MS = 90_000;

  private final ChatWebSocket chat;

  public ChatWebSocketConfig(ChatWebSocket chat) {
    this.chat = chat;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registro) {
    String frontend = Entorno.opcional("FRONTEND_URL", Entorno.esProduccion() ? "" : "http://localhost:4300");
    registro.addHandler(chat, "/ws/chat")
        .setAllowedOriginPatterns(WebConfig.origenesPermitidos(frontend).toArray(String[]::new));
  }

  @Bean
  ServletServerContainerFactoryBean contenedorWebSocket() {
    ServletServerContainerFactoryBean contenedor = new ServletServerContainerFactoryBean();
    contenedor.setMaxTextMessageBufferSize(TAMANO_MAXIMO_MENSAJE);
    contenedor.setMaxSessionIdleTimeout(ESPERA_MAXIMA_MS);
    return contenedor;
  }
}
