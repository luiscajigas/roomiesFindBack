package com.roomiesfind.chat.eventos;

import com.google.protobuf.InvalidProtocolBufferException;
import com.roomiesfind.contratos.chat.EventoChat;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/** Reparte lo que llega de Redis a los streams gRPC abiertos en esta instancia del servicio. */
@Component
public class SuscriptoresEventos implements MessageListener {

  public static final String PATRON_CANALES = "chat:usuario:*";
  private static final String PREFIJO_CANAL = "chat:usuario:";
  private static final Logger log = LoggerFactory.getLogger(SuscriptoresEventos.class);

  private final Map<Integer, Set<StreamObserver<EventoChat>>> streams = new ConcurrentHashMap<>();

  public static String canalDe(int usuarioId) {
    return PREFIJO_CANAL + usuarioId;
  }

  public void agregar(int usuarioId, StreamObserver<EventoChat> stream) {
    streams.computeIfAbsent(usuarioId, (id) -> ConcurrentHashMap.newKeySet()).add(stream);
  }

  public void quitar(int usuarioId, StreamObserver<EventoChat> stream) {
    streams.computeIfPresent(usuarioId, (id, abiertos) -> {
      abiertos.remove(stream);
      return abiertos.isEmpty() ? null : abiertos;
    });
  }

  public int abiertos(int usuarioId) {
    Set<StreamObserver<EventoChat>> abiertos = streams.get(usuarioId);
    return abiertos == null ? 0 : abiertos.size();
  }

  @Override
  public void onMessage(Message mensaje, byte[] patron) {
    String canal = new String(mensaje.getChannel(), StandardCharsets.UTF_8);
    if (!canal.startsWith(PREFIJO_CANAL)) return;
    try {
      entregar(Integer.parseInt(canal.substring(PREFIJO_CANAL.length())), EventoChat.parseFrom(mensaje.getBody()));
    } catch (NumberFormatException | InvalidProtocolBufferException error) {
      log.warn("Se ignoró un evento inválido de Redis en {}", canal);
    }
  }

  void entregar(int usuarioId, EventoChat evento) {
    Set<StreamObserver<EventoChat>> abiertos = streams.get(usuarioId);
    if (abiertos == null) return;
    for (StreamObserver<EventoChat> stream : abiertos) {
      try {
        // Un StreamObserver no admite llamadas simultáneas.
        synchronized (stream) {
          stream.onNext(evento);
        }
      } catch (RuntimeException error) {
        quitar(usuarioId, stream);
      }
    }
  }
}
