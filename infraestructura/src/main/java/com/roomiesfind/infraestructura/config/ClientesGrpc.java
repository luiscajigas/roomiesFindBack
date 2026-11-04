package com.roomiesfind.infraestructura.config;

import com.roomiesfind.contratos.compatibilidad.CompatibilidadGrpc;
import com.roomiesfind.contratos.perfiles.PerfilesGrpc;
import com.roomiesfind.contratos.usuarios.UsuariosGrpc;
import io.grpc.Channel;
import java.util.concurrent.TimeUnit;

/**
 * Clientes gRPC hacia los microservicios. Cada llamada tiene tiempo límite y, si
 * el servicio está arrancando, espera a que esté listo en vez de fallar enseguida.
 */
public class ClientesGrpc {

  private static final int ESPERA_SEGUNDOS = 20;
  /** Debe superar la espera de compatibilidad hacia IA, que incluye a los proveedores externos. */
  private static final int ESPERA_COMPATIBILIDAD_SEGUNDOS = 200;

  private final UsuariosGrpc.UsuariosBlockingStub usuarios;
  private final PerfilesGrpc.PerfilesBlockingStub perfiles;
  private final CompatibilidadGrpc.CompatibilidadBlockingStub compatibilidad;

  public ClientesGrpc(Channel usuarios, Channel perfiles, Channel compatibilidad) {
    this.usuarios = UsuariosGrpc.newBlockingStub(usuarios).withWaitForReady();
    this.perfiles = PerfilesGrpc.newBlockingStub(perfiles).withWaitForReady();
    this.compatibilidad = CompatibilidadGrpc.newBlockingStub(compatibilidad).withWaitForReady();
  }

  public UsuariosGrpc.UsuariosBlockingStub usuarios() {
    return usuarios.withDeadlineAfter(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
  }

  public PerfilesGrpc.PerfilesBlockingStub perfiles() {
    return perfiles.withDeadlineAfter(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
  }

  public CompatibilidadGrpc.CompatibilidadBlockingStub compatibilidad() {
    return compatibilidad.withDeadlineAfter(ESPERA_COMPATIBILIDAD_SEGUNDOS, TimeUnit.SECONDS);
  }
}
