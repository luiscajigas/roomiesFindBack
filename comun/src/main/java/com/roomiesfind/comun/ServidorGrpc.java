package com.roomiesfind.comun;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Servidor gRPC de un microservicio. El transporte Netty de gRPC siempre usa
 * HTTP/2; dentro de la red interna va en texto plano (h2c).
 */
public class ServidorGrpc implements SmartLifecycle {

  private static final Logger log = LoggerFactory.getLogger(ServidorGrpc.class);

  /** Una foto de 5 MB más el resto del mensaje; el límite de gRPC por defecto es 4 MB. */
  public static final int TAMANO_MAXIMO_MENSAJE = 8 * 1024 * 1024;

  private final int puerto;
  private final BindableService api;
  private Server servidor;

  public ServidorGrpc(int puerto, BindableService api) {
    this.puerto = puerto;
    this.api = api;
  }

  @Override
  public void start() {
    try {
      servidor = NettyServerBuilder.forPort(puerto)
          .maxInboundMessageSize(TAMANO_MAXIMO_MENSAJE)
          .addService(api)
          .build()
          .start();
    } catch (IOException error) {
      throw new UncheckedIOException("No se pudo abrir el puerto gRPC " + puerto, error);
    }
    log.info("gRPC sobre HTTP/2 escuchando en el puerto {}", puerto);

    // Los hilos de gRPC son daemon: este mantiene viva la aplicación.
    Thread espera = new Thread(this::esperarTerminacion, "grpc-espera");
    espera.setDaemon(false);
    espera.start();
  }

  private void esperarTerminacion() {
    try {
      servidor.awaitTermination();
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void stop() {
    if (servidor == null) return;
    servidor.shutdown();
    try {
      servidor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
    }
    servidor.shutdownNow();
  }

  @Override
  public boolean isRunning() {
    return servidor != null && !servidor.isShutdown();
  }
}
