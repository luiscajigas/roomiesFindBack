package com.roomiesfind.chat.api;

import com.google.protobuf.Empty;
import com.roomiesfind.chat.controller.ChatController;
import com.roomiesfind.chat.dto.ConversacionDto;
import com.roomiesfind.chat.dto.PaginaMensajesDto;
import com.roomiesfind.chat.eventos.SuscriptoresEventos;
import com.roomiesfind.chat.eventos.TraductorEventos;
import com.roomiesfind.comun.RespuestaGrpc;
import com.roomiesfind.contratos.chat.ChatGrpc;
import com.roomiesfind.contratos.chat.Conversacion;
import com.roomiesfind.contratos.chat.EnviarMensajeRequest;
import com.roomiesfind.contratos.chat.EscucharEventosRequest;
import com.roomiesfind.contratos.chat.EventoChat;
import com.roomiesfind.contratos.chat.ListarConversacionesRequest;
import com.roomiesfind.contratos.chat.ListarConversacionesResponse;
import com.roomiesfind.contratos.chat.MarcarLeidosRequest;
import com.roomiesfind.contratos.chat.Mensaje;
import com.roomiesfind.contratos.chat.NotificarEscribiendoRequest;
import com.roomiesfind.contratos.chat.ObtenerMensajesRequest;
import com.roomiesfind.contratos.chat.ObtenerMensajesResponse;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

/**
 * API del microservicio: lo único que ven los demás módulos, y solo por gRPC.
 * Traduce entre los mensajes protobuf y los DTO de la capa interna.
 */
@Component
public class ChatApi extends ChatGrpc.ChatImplBase {

  private final ChatController controller;
  private final SuscriptoresEventos suscriptores;

  public ChatApi(ChatController controller, SuscriptoresEventos suscriptores) {
    this.controller = controller;
    this.suscriptores = suscriptores;
  }

  @Override
  public void listarConversaciones(ListarConversacionesRequest request, StreamObserver<ListarConversacionesResponse> observador) {
    RespuestaGrpc.responder(observador, () -> ListarConversacionesResponse.newBuilder()
        .addAllConversaciones(controller.conversaciones(request.getUsuarioId()).stream().map(ChatApi::aConversacion).toList())
        .build());
  }

  @Override
  public void obtenerMensajes(ObtenerMensajesRequest request, StreamObserver<ObtenerMensajesResponse> observador) {
    RespuestaGrpc.responder(observador, () -> {
      PaginaMensajesDto pagina = controller.mensajes(
          request.getUsuarioId(), request.getOtroId(), request.hasAntesDe() ? request.getAntesDe() : null, request.getLimite());
      return ObtenerMensajesResponse.newBuilder()
          .addAllMensajes(pagina.mensajes().stream().map(TraductorEventos::aMensaje).toList())
          .setHayMas(pagina.hayMas())
          .build();
    });
  }

  @Override
  public void enviarMensaje(EnviarMensajeRequest request, StreamObserver<Mensaje> observador) {
    RespuestaGrpc.responder(observador, () -> TraductorEventos.aMensaje(
        controller.enviar(request.getRemitenteId(), request.getDestinatarioId(), request.getContenido())));
  }

  @Override
  public void marcarLeidos(MarcarLeidosRequest request, StreamObserver<Empty> observador) {
    RespuestaGrpc.responder(observador, () -> {
      controller.marcarLeidos(request.getUsuarioId(), request.getOtroId());
      return Empty.getDefaultInstance();
    });
  }

  @Override
  public void notificarEscribiendo(NotificarEscribiendoRequest request, StreamObserver<Empty> observador) {
    RespuestaGrpc.responder(observador, () -> {
      controller.notificarEscribiendo(request.getUsuarioId(), request.getDestinatarioId());
      return Empty.getDefaultInstance();
    });
  }

  /** No se responde y termina: el stream queda abierto hasta que el orquestador lo cancela. */
  @Override
  public void escucharEventos(EscucharEventosRequest request, StreamObserver<EventoChat> observador) {
    ServerCallStreamObserver<EventoChat> stream = (ServerCallStreamObserver<EventoChat>) observador;
    int usuarioId = request.getUsuarioId();
    stream.setOnCancelHandler(() -> suscriptores.quitar(usuarioId, observador));
    suscriptores.agregar(usuarioId, observador);
  }

  private static Conversacion aConversacion(ConversacionDto conversacion) {
    return Conversacion.newBuilder()
        .setOtroId(conversacion.otroId())
        .setUltimoMensaje(TraductorEventos.aMensaje(conversacion.ultimoMensaje()))
        .setNoLeidos(conversacion.noLeidos())
        .build();
  }
}
