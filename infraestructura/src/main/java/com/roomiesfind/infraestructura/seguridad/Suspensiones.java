package com.roomiesfind.infraestructura.seguridad;

import com.roomiesfind.contratos.moderacion.UsuarioRequest;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Dice si una cuenta está suspendida. Se consulta en cada petición, así que guarda la
 * respuesta unos segundos; al suspender o reactivar desde el panel se olvida al instante.
 */
@Component
public class Suspensiones {

  private static final long VIGENCIA_MS = 30_000;

  private record Respuesta(boolean suspendida, long hasta) {}

  private final ClientesGrpc clientes;
  private final Map<Integer, Respuesta> recientes = new ConcurrentHashMap<>();

  public Suspensiones(ClientesGrpc clientes) {
    this.clientes = clientes;
  }

  public boolean estaSuspendida(int usuarioId) {
    long ahora = System.currentTimeMillis();
    Respuesta guardada = recientes.get(usuarioId);
    if (guardada != null && guardada.hasta() > ahora) return guardada.suspendida();
    boolean suspendida = clientes.moderacion()
        .estaSuspendida(UsuarioRequest.newBuilder().setUsuarioId(usuarioId).build())
        .getSuspendida();
    recientes.put(usuarioId, new Respuesta(suspendida, ahora + VIGENCIA_MS));
    return suspendida;
  }

  public void olvidar(int usuarioId) {
    recientes.remove(usuarioId);
  }
}
