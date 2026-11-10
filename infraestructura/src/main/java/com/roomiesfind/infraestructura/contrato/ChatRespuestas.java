package com.roomiesfind.infraestructura.contrato;

import com.roomiesfind.contratos.chat.Mensaje;
import java.util.List;

/** Respuestas REST del chat. Lo que ocurre en vivo va por el WebSocket (ver EventoNavegador). */
public final class ChatRespuestas {

  private ChatRespuestas() {}

  /** @param leidoEn null mientras no se ha leído */
  public record MensajeRespuesta(long id, int remitenteId, int destinatarioId, String contenido, String creadoEn, String leidoEn) {

    public static MensajeRespuesta de(Mensaje mensaje) {
      return new MensajeRespuesta(mensaje.getId(), mensaje.getRemitenteId(), mensaje.getDestinatarioId(),
          mensaje.getContenido(), mensaje.getCreadoEn(), mensaje.hasLeidoEn() ? mensaje.getLeidoEn() : null);
    }
  }

  public record ConversacionRespuesta(int otroId, PersonaRespuesta persona, MensajeRespuesta ultimoMensaje, int noLeidos) {}

  public record PaginaMensajesRespuesta(List<MensajeRespuesta> mensajes, boolean hayMas) {}
}
