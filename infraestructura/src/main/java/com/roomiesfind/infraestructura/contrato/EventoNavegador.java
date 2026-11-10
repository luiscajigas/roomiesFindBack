package com.roomiesfind.infraestructura.contrato;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.roomiesfind.contratos.chat.EventoChat;
import com.roomiesfind.infraestructura.contrato.ChatRespuestas.MensajeRespuesta;

/**
 * Mensajes que el orquestador envía al navegador por el WebSocket. Es una jerarquía cerrada
 * (sealed): el compilador obliga a manejar cada tipo, y Jackson escribe el campo "tipo"
 * que el frontend usa para distinguirlos (EventoChat en chat.service.ts).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
  @JsonSubTypes.Type(value = EventoNavegador.Listo.class, name = "listo"),
  @JsonSubTypes.Type(value = EventoNavegador.Pong.class, name = "pong"),
  @JsonSubTypes.Type(value = EventoNavegador.Enviado.class, name = "enviado"),
  @JsonSubTypes.Type(value = EventoNavegador.ErrorEnvio.class, name = "error"),
  @JsonSubTypes.Type(value = EventoNavegador.MensajeNuevo.class, name = "mensaje"),
  @JsonSubTypes.Type(value = EventoNavegador.Leidos.class, name = "leidos"),
  @JsonSubTypes.Type(value = EventoNavegador.Escribiendo.class, name = "escribiendo"),
  @JsonSubTypes.Type(value = EventoNavegador.Alerta.class, name = "alerta"),
  @JsonSubTypes.Type(value = EventoNavegador.Bloqueo.class, name = "bloqueo")
})
public sealed interface EventoNavegador {

  record Listo(int usuarioId) implements EventoNavegador {}

  record Pong() implements EventoNavegador {}

  record Enviado(String idCliente, MensajeRespuesta mensaje) implements EventoNavegador {}

  /** @param idCliente null si el error no corresponde a un mensaje enviado */
  record ErrorEnvio(String idCliente, String mensaje) implements EventoNavegador {}

  record MensajeNuevo(MensajeRespuesta mensaje) implements EventoNavegador {}

  record Leidos(int lectorId, int remitenteId, String leidoEn) implements EventoNavegador {}

  record Escribiendo(int usuarioId) implements EventoNavegador {}

  record Alerta(int usuarioId) implements EventoNavegador {}

  record Bloqueo(int usuarioId) implements EventoNavegador {}

  /** Traduce un evento gRPC del microservicio de chat. Devuelve null si es de un tipo que este orquestador no conoce. */
  static EventoNavegador de(EventoChat evento) {
    return switch (evento.getEventoCase()) {
      case MENSAJE -> new MensajeNuevo(MensajeRespuesta.de(evento.getMensaje()));
      case LEIDOS -> new Leidos(evento.getLeidos().getLectorId(), evento.getLeidos().getRemitenteId(), evento.getLeidos().getLeidoEn());
      case ESCRIBIENDO -> new Escribiendo(evento.getEscribiendo().getUsuarioId());
      case ALERTA -> new Alerta(evento.getAlerta().getUsuarioId());
      case BLOQUEO -> new Bloqueo(evento.getBloqueo().getUsuarioId());
      case EVENTO_NOT_SET -> null;
    };
  }
}
