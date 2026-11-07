package com.roomiesfind.chat.eventos;

import com.roomiesfind.chat.dto.MensajeDto;
import com.roomiesfind.contratos.chat.AlertaCuenta;
import com.roomiesfind.contratos.chat.BloqueoCambiado;
import com.roomiesfind.contratos.chat.EventoChat;
import com.roomiesfind.contratos.chat.Mensaje;
import com.roomiesfind.contratos.chat.MensajesLeidos;
import com.roomiesfind.contratos.chat.PersonaEscribiendo;

/** Arma los mensajes protobuf que viajan por gRPC y por Redis. */
public final class TraductorEventos {

  private TraductorEventos() {}

  public static Mensaje aMensaje(MensajeDto mensaje) {
    Mensaje.Builder respuesta = Mensaje.newBuilder()
        .setId(mensaje.id())
        .setRemitenteId(mensaje.remitenteId())
        .setDestinatarioId(mensaje.destinatarioId())
        .setContenido(mensaje.contenido())
        .setCreadoEn(mensaje.creadoEn());
    if (mensaje.leidoEn() != null) respuesta.setLeidoEn(mensaje.leidoEn());
    return respuesta.build();
  }

  public static EventoChat eventoMensaje(MensajeDto mensaje) {
    return EventoChat.newBuilder().setMensaje(aMensaje(mensaje)).build();
  }

  public static EventoChat eventoLeidos(int lectorId, int remitenteId, String leidoEn) {
    return EventoChat.newBuilder()
        .setLeidos(MensajesLeidos.newBuilder().setLectorId(lectorId).setRemitenteId(remitenteId).setLeidoEn(leidoEn))
        .build();
  }

  public static EventoChat eventoAlerta(int usuarioId) {
    return EventoChat.newBuilder().setAlerta(AlertaCuenta.newBuilder().setUsuarioId(usuarioId)).build();
  }

  public static EventoChat eventoBloqueo(int usuarioId) {
    return EventoChat.newBuilder().setBloqueo(BloqueoCambiado.newBuilder().setUsuarioId(usuarioId)).build();
  }

  public static EventoChat eventoEscribiendo(int usuarioId) {
    return EventoChat.newBuilder().setEscribiendo(PersonaEscribiendo.newBuilder().setUsuarioId(usuarioId)).build();
  }
}
