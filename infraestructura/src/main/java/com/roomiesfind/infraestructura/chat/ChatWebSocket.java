package com.roomiesfind.infraestructura.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomiesfind.contratos.chat.EnviarMensajeRequest;
import com.roomiesfind.contratos.chat.EscucharEventosRequest;
import com.roomiesfind.contratos.chat.EventoChat;
import com.roomiesfind.contratos.chat.MarcarLeidosRequest;
import com.roomiesfind.contratos.chat.Mensaje;
import com.roomiesfind.contratos.chat.NotificarEscribiendoRequest;
import com.roomiesfind.contratos.usuarios.ValidarTokenRequest;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.seguridad.Suspensiones;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Puente entre el navegador y el microservicio de chat: el navegador habla por
 * WebSocket y el resto del camino va por gRPC. Este módulo no conoce Redis.
 *
 * <p>El token llega como primer mensaje y no en la URL, para que no quede en los
 * registros de los servidores. Sin él, la conexión se cierra a los pocos segundos.
 */
@Component
public class ChatWebSocket extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(ChatWebSocket.class);
  private static final int SEGUNDOS_PARA_AUTENTICAR = 5;
  private static final int ESPERA_ENVIO_MS = 5_000;
  private static final int BUFFER_ENVIO_BYTES = 256 * 1024;

  private final ClientesGrpc clientes;
  private final ObjectMapper json;
  private final Suspensiones suspensiones;
  private final Map<String, ConexionChat> conexiones = new ConcurrentHashMap<>();
  private final ScheduledExecutorService temporizador = Executors.newSingleThreadScheduledExecutor((tarea) -> {
    Thread hilo = new Thread(tarea, "chat-autenticacion");
    hilo.setDaemon(true);
    return hilo;
  });

  public ChatWebSocket(ClientesGrpc clientes, ObjectMapper json, Suspensiones suspensiones) {
    this.clientes = clientes;
    this.json = json;
    this.suspensiones = suspensiones;
  }

  /** Cierra todas las conexiones de una persona, por ejemplo al suspender su cuenta. */
  public void cerrarConexionesDe(int usuarioId) {
    conexiones.values().stream()
        .filter((conexion) -> Integer.valueOf(usuarioId).equals(conexion.usuarioId))
        .forEach((conexion) -> cerrar(conexion, CloseStatus.POLICY_VIOLATION));
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession sesion) {
    ConexionChat conexion = new ConexionChat(new ConcurrentWebSocketSessionDecorator(sesion, ESPERA_ENVIO_MS, BUFFER_ENVIO_BYTES));
    conexiones.put(sesion.getId(), conexion);
    temporizador.schedule(() -> {
      if (!conexion.autenticada()) cerrar(conexion, CloseStatus.POLICY_VIOLATION);
    }, SEGUNDOS_PARA_AUTENTICAR, TimeUnit.SECONDS);
  }

  @Override
  protected void handleTextMessage(WebSocketSession sesion, TextMessage mensaje) throws IOException {
    ConexionChat conexion = conexiones.get(sesion.getId());
    if (conexion == null) return;

    JsonNode datos;
    try {
      datos = json.readTree(mensaje.getPayload());
    } catch (IOException error) {
      cerrar(conexion, CloseStatus.BAD_DATA);
      return;
    }

    String tipo = datos.path("tipo").asText("");
    if (!conexion.autenticada()) {
      if ("autenticar".equals(tipo)) autenticar(conexion, datos.path("token").asText(""));
      else cerrar(conexion, CloseStatus.POLICY_VIOLATION);
      return;
    }

    try {
      switch (tipo) {
        case "enviar" -> enviar(conexion, datos);
        case "leer" -> clientes.chat().marcarLeidos(MarcarLeidosRequest.newBuilder()
            .setUsuarioId(conexion.usuarioId).setOtroId(datos.path("otroId").asInt()).build());
        case "escribiendo" -> clientes.chat().notificarEscribiendo(NotificarEscribiendoRequest.newBuilder()
            .setUsuarioId(conexion.usuarioId).setDestinatarioId(datos.path("destinatarioId").asInt()).build());
        case "ping" -> responder(conexion, Map.of("tipo", "pong"));
        default -> cerrar(conexion, CloseStatus.BAD_DATA);
      }
    } catch (StatusRuntimeException error) {
      enviarError(conexion, datos.path("idCliente").asText(null), error.getStatus());
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession sesion, CloseStatus estado) {
    ConexionChat conexion = conexiones.remove(sesion.getId());
    if (conexion != null) conexion.cerrarStream();
  }

  private void autenticar(ConexionChat conexion, String token) {
    try {
      conexion.usuarioId = clientes.usuarios()
          .validarToken(ValidarTokenRequest.newBuilder().setToken(token).build())
          .getUsuarioId();
    } catch (StatusRuntimeException error) {
      cerrar(conexion, CloseStatus.POLICY_VIOLATION);
      return;
    }
    if (suspensiones.estaSuspendida(conexion.usuarioId)) {
      cerrar(conexion, CloseStatus.POLICY_VIOLATION);
      return;
    }
    escucharEventos(conexion);
    responder(conexion, Map.of("tipo", "listo", "usuarioId", conexion.usuarioId));
  }

  /** Abre el stream gRPC. Si se corta, cierra el WebSocket: el navegador reconecta y se pone al día. */
  private void escucharEventos(ConexionChat conexion) {
    clientes.chatEnVivo().escucharEventos(
        EscucharEventosRequest.newBuilder().setUsuarioId(conexion.usuarioId).build(),
        new ClientResponseObserver<EscucharEventosRequest, EventoChat>() {
          @Override
          public void beforeStart(ClientCallStreamObserver<EscucharEventosRequest> stream) {
            conexion.stream = stream;
          }

          @Override
          public void onNext(EventoChat evento) {
            responder(conexion, aMapa(evento));
          }

          @Override
          public void onError(Throwable error) {
            // No basta con ignorar CANCELLED: es lo que llega también cuando el servicio se apaga.
            // Si el WebSocket ya estaba cerrado, cerrar no hace nada.
            if (conexion.sesion.isOpen()) {
              log.warn("Se cortó el stream de chat: {}", Status.fromThrowable(error).getCode());
              cerrar(conexion, CloseStatus.SERVICE_RESTARTED);
            }
          }

          @Override
          public void onCompleted() {
            cerrar(conexion, CloseStatus.SERVICE_RESTARTED);
          }
        });
  }

  private void enviar(ConexionChat conexion, JsonNode datos) {
    String idCliente = datos.path("idCliente").asText("");
    if (!conexion.puedeEnviar(System.currentTimeMillis())) {
      enviarError(conexion, idCliente, Status.RESOURCE_EXHAUSTED);
      return;
    }
    Mensaje guardado = clientes.chat().enviarMensaje(EnviarMensajeRequest.newBuilder()
        .setRemitenteId(conexion.usuarioId)
        .setDestinatarioId(datos.path("destinatarioId").asInt())
        .setContenido(datos.path("contenido").asText(""))
        .build());
    Map<String, Object> respuesta = new LinkedHashMap<>();
    respuesta.put("tipo", "enviado");
    respuesta.put("idCliente", idCliente);
    respuesta.put("mensaje", ChatRest.aMapa(guardado));
    responder(conexion, respuesta);
  }

  private void enviarError(ConexionChat conexion, String idCliente, Status status) {
    Map<String, Object> respuesta = new LinkedHashMap<>();
    respuesta.put("tipo", "error");
    respuesta.put("idCliente", idCliente);
    respuesta.put("mensaje", mensajeDeError(status));
    responder(conexion, respuesta);
  }

  static String mensajeDeError(Status status) {
    return switch (status.getCode()) {
      case INVALID_ARGUMENT, NOT_FOUND -> status.getDescription();
      case RESOURCE_EXHAUSTED -> "Estás enviando mensajes muy rápido. Espera unos segundos.";
      default -> "No se pudo enviar el mensaje. Inténtalo de nuevo.";
    };
  }

  private static Map<String, Object> aMapa(EventoChat evento) {
    Map<String, Object> mapa = new LinkedHashMap<>();
    switch (evento.getEventoCase()) {
      case MENSAJE -> {
        mapa.put("tipo", "mensaje");
        mapa.put("mensaje", ChatRest.aMapa(evento.getMensaje()));
      }
      case LEIDOS -> {
        mapa.put("tipo", "leidos");
        mapa.put("lectorId", evento.getLeidos().getLectorId());
        mapa.put("remitenteId", evento.getLeidos().getRemitenteId());
        mapa.put("leidoEn", evento.getLeidos().getLeidoEn());
      }
      case ESCRIBIENDO -> {
        mapa.put("tipo", "escribiendo");
        mapa.put("usuarioId", evento.getEscribiendo().getUsuarioId());
      }
      case ALERTA -> {
        mapa.put("tipo", "alerta");
        mapa.put("usuarioId", evento.getAlerta().getUsuarioId());
      }
      case BLOQUEO -> {
        mapa.put("tipo", "bloqueo");
        mapa.put("usuarioId", evento.getBloqueo().getUsuarioId());
      }
      default -> mapa.put("tipo", "desconocido");
    }
    return mapa;
  }

  private void responder(ConexionChat conexion, Map<String, ?> datos) {
    try {
      if (conexion.sesion.isOpen()) conexion.sesion.sendMessage(new TextMessage(json.writeValueAsString(datos)));
    } catch (IOException | IllegalStateException error) {
      log.debug("No se pudo escribir en el WebSocket: {}", error.getMessage());
      cerrar(conexion, CloseStatus.SESSION_NOT_RELIABLE);
    }
  }

  private void cerrar(ConexionChat conexion, CloseStatus estado) {
    try {
      if (conexion.sesion.isOpen()) conexion.sesion.close(estado);
    } catch (IOException error) {
      log.debug("No se pudo cerrar el WebSocket: {}", error.getMessage());
    }
  }
}
