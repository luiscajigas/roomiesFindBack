package com.roomiesfind.infraestructura.chat;

import com.roomiesfind.contratos.chat.Conversacion;
import com.roomiesfind.contratos.chat.ListarConversacionesRequest;
import com.roomiesfind.contratos.chat.Mensaje;
import com.roomiesfind.contratos.chat.ObtenerMensajesRequest;
import com.roomiesfind.contratos.chat.ObtenerMensajesResponse;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.rest.ErrorRest;
import com.roomiesfind.infraestructura.seguridad.AutenticacionInterceptor;
import com.roomiesfind.infraestructura.seguridad.RequiereAuth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Historial del chat para el frontend. Lo que pasa en vivo va por el WebSocket. */
@RestController
@RequestMapping("/api/chat")
public class ChatRest {

  private final ClientesGrpc clientes;

  public ChatRest(ClientesGrpc clientes) {
    this.clientes = clientes;
  }

  @RequiereAuth
  @GetMapping("/conversaciones")
  List<Map<String, Object>> conversaciones(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId) {
    return clientes.chat()
        .listarConversaciones(ListarConversacionesRequest.newBuilder().setUsuarioId(usuarioId).build())
        .getConversacionesList().stream()
        .map(ChatRest::aMapa)
        .toList();
  }

  @RequiereAuth
  @GetMapping("/conversaciones/{otroId}/mensajes")
  Map<String, Object> mensajes(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId,
      @PathVariable String otroId,
      @RequestParam(required = false) String antesDe) {
    ObtenerMensajesRequest.Builder peticion = ObtenerMensajesRequest.newBuilder()
        .setUsuarioId(usuarioId)
        .setOtroId((int) leerNumero(otroId, Integer.MAX_VALUE));
    if (antesDe != null) peticion.setAntesDe(leerNumero(antesDe, Long.MAX_VALUE));

    ObtenerMensajesResponse pagina = clientes.chat().obtenerMensajes(peticion.build());
    Map<String, Object> respuesta = new LinkedHashMap<>();
    respuesta.put("mensajes", pagina.getMensajesList().stream().map(ChatRest::aMapa).toList());
    respuesta.put("hayMas", pagina.getHayMas());
    return respuesta;
  }

  private static long leerNumero(String texto, long maximo) {
    try {
      long numero = Long.parseLong(texto);
      if (numero > 0 && numero <= maximo) return numero;
    } catch (NumberFormatException error) {
      // Se responde abajo como solicitud inválida.
    }
    throw new ErrorRest(HttpStatus.BAD_REQUEST, "La solicitud no es válida.");
  }

  private static Map<String, Object> aMapa(Conversacion conversacion) {
    Map<String, Object> mapa = new LinkedHashMap<>();
    mapa.put("otroId", conversacion.getOtroId());
    mapa.put("ultimoMensaje", aMapa(conversacion.getUltimoMensaje()));
    mapa.put("noLeidos", conversacion.getNoLeidos());
    return mapa;
  }

  static Map<String, Object> aMapa(Mensaje mensaje) {
    Map<String, Object> mapa = new LinkedHashMap<>();
    mapa.put("id", mensaje.getId());
    mapa.put("remitenteId", mensaje.getRemitenteId());
    mapa.put("destinatarioId", mensaje.getDestinatarioId());
    mapa.put("contenido", mensaje.getContenido());
    mapa.put("creadoEn", mensaje.getCreadoEn());
    mapa.put("leidoEn", mensaje.hasLeidoEn() ? mensaje.getLeidoEn() : null);
    return mapa;
  }
}
