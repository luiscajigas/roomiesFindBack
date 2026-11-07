package com.roomiesfind.chat.cliente;

import com.roomiesfind.contratos.moderacion.ModeracionGrpc;
import com.roomiesfind.contratos.moderacion.ParejaRequest;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** Cliente gRPC del microservicio de moderación. */
@Component
public class ModeracionCliente {

  private static final int ESPERA_SEGUNDOS = 3;

  private final ModeracionGrpc.ModeracionBlockingStub moderacion;

  public ModeracionCliente(ModeracionGrpc.ModeracionBlockingStub moderacion) {
    this.moderacion = moderacion;
  }

  public boolean estanBloqueados(int usuarioId, int otroId) {
    return moderacion.withDeadlineAfter(ESPERA_SEGUNDOS, TimeUnit.SECONDS)
        .estanBloqueados(ParejaRequest.newBuilder().setUsuarioId(usuarioId).setOtroId(otroId).build())
        .getBloqueados();
  }
}
