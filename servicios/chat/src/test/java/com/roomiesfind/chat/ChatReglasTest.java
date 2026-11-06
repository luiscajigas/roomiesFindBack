package com.roomiesfind.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.roomiesfind.chat.controller.ChatController;
import com.roomiesfind.chat.dto.MensajeDto;
import com.roomiesfind.chat.eventos.PublicadorEventos;
import com.roomiesfind.chat.eventos.SuscriptoresEventos;
import com.roomiesfind.chat.eventos.TraductorEventos;
import com.roomiesfind.chat.service.ChatService;
import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.contratos.chat.EventoChat;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;

class ChatReglasTest {

  private static class StreamFalso implements StreamObserver<EventoChat> {
    final List<EventoChat> recibidos = new ArrayList<>();
    boolean roto;

    @Override public void onNext(EventoChat evento) {
      if (roto) throw new IllegalStateException("cancelado");
      recibidos.add(evento);
    }
    @Override public void onError(Throwable error) {}
    @Override public void onCompleted() {}
  }

  private static DefaultMessage deRedis(int usuarioId, EventoChat evento) {
    return new DefaultMessage(SuscriptoresEventos.canalDe(usuarioId).getBytes(StandardCharsets.UTF_8), evento.toByteArray());
  }

  private static final MensajeDto MENSAJE = new MensajeDto(9, 1, 2, "Hola", "2026-11-06T10:00:00Z", null);

  @Test
  void validaAntesDeGuardar() {
    ChatController controller = new ChatController((ChatService) null, (PublicadorEventos) null);
    assertEquals("No puedes enviar mensajes a tu propia cuenta.",
        assertThrows(ErrorNegocio.class, () -> controller.enviar(1, 1, "Hola")).getMessage());
    assertEquals("La persona no existe.",
        assertThrows(ErrorNegocio.class, () -> controller.enviar(1, 0, "Hola")).getMessage());
    assertEquals("Escribe un mensaje antes de enviarlo.",
        assertThrows(ErrorNegocio.class, () -> controller.enviar(1, 2, "   ")).getMessage());
    assertEquals("El mensaje puede tener máximo 2000 caracteres.",
        assertThrows(ErrorNegocio.class, () -> controller.enviar(1, 2, "x".repeat(2001))).getMessage());
  }

  @Test
  void entregaLoDeRedisSoloALaPersonaDelCanal() {
    SuscriptoresEventos suscriptores = new SuscriptoresEventos();
    StreamFalso deAna = new StreamFalso();
    StreamFalso deBeto = new StreamFalso();
    suscriptores.agregar(1, deAna);
    suscriptores.agregar(2, deBeto);

    suscriptores.onMessage(deRedis(2, TraductorEventos.eventoMensaje(MENSAJE)), null);

    assertTrue(deAna.recibidos.isEmpty());
    assertEquals("Hola", deBeto.recibidos.get(0).getMensaje().getContenido());
  }

  @Test
  void entregaATodasLasPestanasDeUnaPersona() {
    SuscriptoresEventos suscriptores = new SuscriptoresEventos();
    StreamFalso pestanaUno = new StreamFalso();
    StreamFalso pestanaDos = new StreamFalso();
    suscriptores.agregar(2, pestanaUno);
    suscriptores.agregar(2, pestanaDos);

    suscriptores.onMessage(deRedis(2, TraductorEventos.eventoEscribiendo(1)), null);

    assertEquals(1, pestanaUno.recibidos.size());
    assertEquals(1, pestanaDos.recibidos.size());
  }

  @Test
  void quitaLosStreamsCerradosSinAfectarALosDemas() {
    SuscriptoresEventos suscriptores = new SuscriptoresEventos();
    StreamFalso cerrado = new StreamFalso();
    cerrado.roto = true;
    StreamFalso abierto = new StreamFalso();
    suscriptores.agregar(2, cerrado);
    suscriptores.agregar(2, abierto);

    suscriptores.onMessage(deRedis(2, TraductorEventos.eventoEscribiendo(1)), null);

    assertEquals(1, abierto.recibidos.size());
    assertEquals(1, suscriptores.abiertos(2));
    suscriptores.quitar(2, abierto);
    assertEquals(0, suscriptores.abiertos(2));
  }

  @Test
  void ignoraEventosInvalidosDeRedis() {
    SuscriptoresEventos suscriptores = new SuscriptoresEventos();
    StreamFalso stream = new StreamFalso();
    suscriptores.agregar(2, stream);

    suscriptores.onMessage(new DefaultMessage("chat:usuario:abc".getBytes(StandardCharsets.UTF_8), new byte[] {1}), null);
    suscriptores.onMessage(new DefaultMessage("otro:canal".getBytes(StandardCharsets.UTF_8), new byte[] {1}), null);

    assertTrue(stream.recibidos.isEmpty());
  }

  @Test
  void elMensajeSinLeerNoLlevaFechaDeLectura() {
    assertFalse(TraductorEventos.aMensaje(MENSAJE).hasLeidoEn());
    assertTrue(TraductorEventos.aMensaje(new MensajeDto(9, 1, 2, "Hola", "x", "y")).hasLeidoEn());
  }
}
