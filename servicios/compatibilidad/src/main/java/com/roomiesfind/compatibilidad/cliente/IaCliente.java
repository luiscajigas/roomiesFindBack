package com.roomiesfind.compatibilidad.cliente;

import com.roomiesfind.contratos.ia.GenerarExplicacionRequest;
import com.roomiesfind.contratos.ia.IaGrpc;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** Cliente gRPC del microservicio de IA. */
@Component
public class IaCliente {

  /** Alcanza para los reintentos del proveedor principal y luego los del respaldo. */
  private static final int ESPERA_MAXIMA_SEGUNDOS = 180;

  private final IaGrpc.IaBlockingStub ia;

  public IaCliente(IaGrpc.IaBlockingStub ia) {
    this.ia = ia;
  }

  public String generar(String nombreUsuario, String nombreCandidato, int score,
      List<String> coincidencias, List<String> conflictos, String idioma) {
    return ia.withDeadlineAfter(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)
        .generarExplicacion(GenerarExplicacionRequest.newBuilder()
            .setNombreUsuario(nombreUsuario)
            .setNombreCandidato(nombreCandidato)
            .setScore(score)
            .addAllCoincidencias(coincidencias)
            .addAllConflictos(conflictos)
            .setIdioma(idioma)
            .build())
        .getTexto();
  }
}
