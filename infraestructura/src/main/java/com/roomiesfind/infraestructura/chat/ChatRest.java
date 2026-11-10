package com.roomiesfind.infraestructura.chat;

import com.roomiesfind.contratos.chat.Conversacion;
import com.roomiesfind.contratos.chat.ListarConversacionesRequest;
import com.roomiesfind.contratos.chat.ObtenerMensajesRequest;
import com.roomiesfind.contratos.chat.ObtenerMensajesResponse;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.contrato.ChatRespuestas.ConversacionRespuesta;
import com.roomiesfind.infraestructura.contrato.ChatRespuestas.MensajeRespuesta;
import com.roomiesfind.infraestructura.contrato.ChatRespuestas.PaginaMensajesRespuesta;
import com.roomiesfind.infraestructura.contrato.PersonaRespuesta;
import com.roomiesfind.infraestructura.orquestacion.ConsultaPerfiles;
import com.roomiesfind.infraestructura.orquestacion.EnParalelo;
import com.roomiesfind.infraestructura.orquestacion.EnParalelo.Par;
import com.roomiesfind.infraestructura.rest.Parametros;
import com.roomiesfind.infraestructura.seguridad.AutenticacionInterceptor;
import com.roomiesfind.infraestructura.seguridad.RequiereAuth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final ConsultaPerfiles perfiles;
  private final EnParalelo enParalelo;

  public ChatRest(ClientesGrpc clientes, ConsultaPerfiles perfiles, EnParalelo enParalelo) {
    this.clientes = clientes;
    this.perfiles = perfiles;
    this.enParalelo = enParalelo;
  }

  @RequiereAuth
  @GetMapping("/conversaciones")
  List<ConversacionRespuesta> conversaciones(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId) {
    Par<List<Conversacion>, Set<Integer>> datos = enParalelo.ambos(
        () -> clientes.chat()
            .listarConversaciones(ListarConversacionesRequest.newBuilder().setUsuarioId(usuarioId).build())
            .getConversacionesList(),
        () -> perfiles.excluidos(usuarioId));
    List<Conversacion> conversaciones = datos.primero();
    Map<Integer, PersonaRespuesta> personas = perfiles
        .personas(conversaciones.stream().map(Conversacion::getOtroId).filter((id) -> !datos.segundo().contains(id)).toList())
        .stream()
        .collect(Collectors.toMap(PersonaRespuesta::id, Function.identity()));

    return conversaciones.stream()
        // Si la otra persona ya no existe o hay un bloqueo, su conversación no se muestra.
        .filter((conversacion) -> personas.containsKey(conversacion.getOtroId()))
        .map((conversacion) -> new ConversacionRespuesta(conversacion.getOtroId(), personas.get(conversacion.getOtroId()),
            MensajeRespuesta.de(conversacion.getUltimoMensaje()), conversacion.getNoLeidos()))
        .toList();
  }

  @RequiereAuth
  @GetMapping("/conversaciones/{otroId}/mensajes")
  PaginaMensajesRespuesta mensajes(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId,
      @PathVariable String otroId,
      @RequestParam(required = false) String antesDe) {
    ObtenerMensajesRequest.Builder peticion = ObtenerMensajesRequest.newBuilder()
        .setUsuarioId(usuarioId)
        .setOtroId(Parametros.id(otroId));
    if (antesDe != null) peticion.setAntesDe(Parametros.idLargo(antesDe));

    ObtenerMensajesResponse pagina = clientes.chat().obtenerMensajes(peticion.build());
    return new PaginaMensajesRespuesta(pagina.getMensajesList().stream().map(MensajeRespuesta::de).toList(), pagina.getHayMas());
  }
}
