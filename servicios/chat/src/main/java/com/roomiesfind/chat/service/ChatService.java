package com.roomiesfind.chat.service;

import com.roomiesfind.chat.dto.ConversacionDto;
import com.roomiesfind.chat.dto.MensajeDto;
import com.roomiesfind.chat.dto.PaginaMensajesDto;
import com.roomiesfind.chat.model.Mensaje;
import com.roomiesfind.chat.repository.ConversacionRepository;
import com.roomiesfind.chat.repository.MensajeRepository;
import com.roomiesfind.comun.ErrorNegocio;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de las conversaciones. Cada consulta filtra por la persona que pregunta:
 * nadie puede leer una conversación en la que no participa.
 */
@Service
public class ChatService {

  private final ConversacionRepository conversaciones;
  private final MensajeRepository mensajes;

  public ChatService(ConversacionRepository conversaciones, MensajeRepository mensajes) {
    this.conversaciones = conversaciones;
    this.mensajes = mensajes;
  }

  public List<ConversacionDto> conversaciones(int usuarioId) {
    return conversaciones.listar(usuarioId);
  }

  public List<Integer> interlocutores(int usuarioId) {
    return conversaciones.interlocutores(usuarioId);
  }

  public PaginaMensajesDto mensajes(int usuarioId, int otroId, Long antesDe, int limite) {
    Optional<Long> conversacion = conversaciones.buscar(usuarioId, otroId);
    if (conversacion.isEmpty()) return new PaginaMensajesDto(List.of(), false);

    // Se pide uno de más para saber si quedan mensajes anteriores.
    List<Mensaje> encontrados = mensajes.listar(conversacion.get(), antesDe, limite + 1);
    boolean hayMas = encontrados.size() > limite;
    List<MensajeDto> pagina = encontrados.stream()
        .limit(limite)
        .map((mensaje) -> aDto(mensaje, destinatarioDe(mensaje, usuarioId, otroId)))
        .toList()
        .reversed();
    return new PaginaMensajesDto(pagina, hayMas);
  }

  @Transactional
  public MensajeDto enviar(int remitenteId, int destinatarioId, String contenido) {
    try {
      long conversacion = conversaciones.obtenerOCrear(remitenteId, destinatarioId);
      return aDto(mensajes.crear(conversacion, remitenteId, contenido), destinatarioId);
    } catch (DataIntegrityViolationException error) {
      throw ErrorNegocio.noEncontrado("La persona no existe.");
    }
  }

  /** @return cuántos mensajes quedaron marcados como leídos */
  @Transactional
  public int marcarLeidos(int usuarioId, int otroId) {
    return conversaciones.buscar(usuarioId, otroId)
        .map((conversacion) -> mensajes.marcarLeidos(conversacion, otroId))
        .orElse(0);
  }

  static int destinatarioDe(Mensaje mensaje, int usuarioId, int otroId) {
    return mensaje.remitenteId() == usuarioId ? otroId : usuarioId;
  }

  private static MensajeDto aDto(Mensaje mensaje, int destinatarioId) {
    return new MensajeDto(mensaje.id(), mensaje.remitenteId(), destinatarioId, mensaje.contenido(),
        mensaje.creadoEn().toString(), mensaje.leidoEn() == null ? null : mensaje.leidoEn().toString());
  }
}
