package com.roomiesfind.chat.controller;

import com.roomiesfind.chat.cliente.ModeracionCliente;
import com.roomiesfind.chat.dto.ConversacionDto;
import com.roomiesfind.chat.dto.MensajeDto;
import com.roomiesfind.chat.dto.PaginaMensajesDto;
import com.roomiesfind.chat.eventos.PublicadorEventos;
import com.roomiesfind.chat.eventos.TraductorEventos;
import com.roomiesfind.chat.service.ChatService;
import com.roomiesfind.comun.ErrorNegocio;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/** Valida la entrada y coordina: primero guarda y, ya guardado, avisa en tiempo real. No conoce gRPC ni SQL. */
@Component
public class ChatController {

  static final int LARGO_MAXIMO = 2000;
  private static final int MENSAJES_POR_PAGINA = 30;
  private static final int MAXIMO_POR_PAGINA = 50;

  private final ChatService chat;
  private final PublicadorEventos eventos;
  private final ModeracionCliente moderacion;

  public ChatController(ChatService chat, PublicadorEventos eventos, ModeracionCliente moderacion) {
    this.chat = chat;
    this.eventos = eventos;
    this.moderacion = moderacion;
  }

  public List<ConversacionDto> conversaciones(int usuarioId) {
    return chat.conversaciones(usuarioId);
  }

  public PaginaMensajesDto mensajes(int usuarioId, int otroId, Long antesDe, int limite) {
    validarOtraPersona(usuarioId, otroId);
    validarSinBloqueo(usuarioId, otroId);
    int tamano = limite <= 0 ? MENSAJES_POR_PAGINA : Math.min(limite, MAXIMO_POR_PAGINA);
    return chat.mensajes(usuarioId, otroId, antesDe, tamano);
  }

  public MensajeDto enviar(int remitenteId, int destinatarioId, String contenido) {
    validarOtraPersona(remitenteId, destinatarioId);
    String texto = validarContenido(contenido);
    validarSinBloqueo(remitenteId, destinatarioId);
    MensajeDto mensaje = chat.enviar(remitenteId, destinatarioId, texto);
    // También va al remitente, para que lo vea en sus otras pestañas.
    eventos.publicar(destinatarioId, TraductorEventos.eventoMensaje(mensaje));
    eventos.publicar(remitenteId, TraductorEventos.eventoMensaje(mensaje));
    return mensaje;
  }

  public void marcarLeidos(int usuarioId, int otroId) {
    validarOtraPersona(usuarioId, otroId);
    if (chat.marcarLeidos(usuarioId, otroId) == 0) return;
    String ahora = Instant.now().toString();
    eventos.publicar(otroId, TraductorEventos.eventoLeidos(usuarioId, otroId, ahora));
    eventos.publicar(usuarioId, TraductorEventos.eventoLeidos(usuarioId, otroId, ahora));
  }

  public void notificarEscribiendo(int usuarioId, int destinatarioId) {
    validarOtraPersona(usuarioId, destinatarioId);
    validarSinBloqueo(usuarioId, destinatarioId);
    eventos.publicar(destinatarioId, TraductorEventos.eventoEscribiendo(usuarioId));
  }

  public void avisarAlerta(int usuarioId) {
    for (int otroId : chat.interlocutores(usuarioId)) {
      eventos.publicar(otroId, TraductorEventos.eventoAlerta(usuarioId));
    }
  }

  public void avisarBloqueo(int usuarioId, int otroId) {
    eventos.publicar(usuarioId, TraductorEventos.eventoBloqueo(otroId));
    eventos.publicar(otroId, TraductorEventos.eventoBloqueo(usuarioId));
  }

  /** Se comprueba aquí y no en el navegador, para que el bloqueo no se pueda saltar. Si no se puede comprobar, no se entrega. */
  private void validarSinBloqueo(int usuarioId, int otroId) {
    if (moderacion.estanBloqueados(usuarioId, otroId)) {
      throw ErrorNegocio.noEncontrado("No puedes conversar con esta persona.");
    }
  }

  static void validarOtraPersona(int usuarioId, int otroId) {
    if (otroId <= 0) {
      throw ErrorNegocio.solicitudInvalida("La persona no existe.");
    }
    if (usuarioId == otroId) {
      throw ErrorNegocio.solicitudInvalida("No puedes enviar mensajes a tu propia cuenta.");
    }
  }

  static String validarContenido(String contenido) {
    String texto = contenido == null ? "" : contenido.strip();
    if (texto.isEmpty()) {
      throw ErrorNegocio.solicitudInvalida("Escribe un mensaje antes de enviarlo.");
    }
    if (texto.length() > LARGO_MAXIMO) {
      throw ErrorNegocio.solicitudInvalida("El mensaje puede tener máximo 2000 caracteres.");
    }
    return texto;
  }
}
