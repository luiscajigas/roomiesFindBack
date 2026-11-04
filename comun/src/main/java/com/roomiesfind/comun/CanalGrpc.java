package com.roomiesfind.comun;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

/** Crea la conexión gRPC (HTTP/2) hacia otro microservicio. */
public final class CanalGrpc {

  private CanalGrpc() {}

  /** @param destino dirección con la forma host:puerto */
  public static ManagedChannel hacia(String destino) {
    return NettyChannelBuilder.forTarget(destino)
        .usePlaintext()
        .maxInboundMessageSize(ServidorGrpc.TAMANO_MAXIMO_MENSAJE)
        .build();
  }
}
