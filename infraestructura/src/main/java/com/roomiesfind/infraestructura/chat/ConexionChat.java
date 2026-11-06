package com.roomiesfind.infraestructura.chat;

import io.grpc.stub.ClientCallStreamObserver;
import org.springframework.web.socket.WebSocketSession;

/** Estado de un WebSocket conectado. */
final class ConexionChat {

  private static final int MAXIMO_MENSAJES = 20;
  private static final long VENTANA_MS = 10_000;

  final WebSocketSession sesion;
  volatile Integer usuarioId;
  volatile ClientCallStreamObserver<?> stream;

  private long inicioVentana = System.currentTimeMillis();
  private int mensajesEnVentana;

  ConexionChat(WebSocketSession sesion) {
    this.sesion = sesion;
  }

  boolean autenticada() {
    return usuarioId != null;
  }

  /** Limita a 20 mensajes cada 10 segundos por conexión. */
  synchronized boolean puedeEnviar(long ahora) {
    if (ahora - inicioVentana >= VENTANA_MS) {
      inicioVentana = ahora;
      mensajesEnVentana = 0;
    }
    return ++mensajesEnVentana <= MAXIMO_MENSAJES;
  }

  void cerrarStream() {
    ClientCallStreamObserver<?> abierto = stream;
    if (abierto != null) abierto.cancel("WebSocket cerrado", null);
  }
}
