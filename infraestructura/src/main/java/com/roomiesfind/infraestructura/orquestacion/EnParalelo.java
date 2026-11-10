package com.roomiesfind.infraestructura.orquestacion;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Hace a la vez llamadas gRPC que no dependen entre sí. Cada tarea corre en un hilo virtual
 * (Java 21): bloquearse esperando la red no ocupa un hilo del sistema operativo, así que
 * crear uno por llamada es barato. La última tarea corre en el hilo actual, que de todos
 * modos tendría que esperar.
 *
 * <p>Si una falla, se propaga su excepción original (por ejemplo un StatusRuntimeException),
 * para que ManejadorErrores la convierta en la respuesta HTTP correcta.
 */
@Component
public class EnParalelo {

  private final ExecutorService hilos = Executors.newVirtualThreadPerTaskExecutor();

  public record Par<A, B>(A primero, B segundo) {}

  public record Trio<A, B, C>(A primero, B segundo, C tercero) {}

  public <A, B> Par<A, B> ambos(Supplier<A> primero, Supplier<B> segundo) {
    Future<A> uno = hilos.submit(primero::get);
    B dos = segundo.get();
    return new Par<>(esperar(uno), dos);
  }

  public <A, B, C> Trio<A, B, C> tres(Supplier<A> primero, Supplier<B> segundo, Supplier<C> tercero) {
    Future<A> uno = hilos.submit(primero::get);
    Future<B> dos = hilos.submit(segundo::get);
    C tres = tercero.get();
    return new Trio<>(esperar(uno), esperar(dos), tres);
  }

  @PreDestroy
  void cerrar() {
    hilos.close();
  }

  private static <T> T esperar(Future<T> tarea) {
    try {
      return tarea.get();
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Se interrumpió una consulta a otro servicio.", error);
    } catch (ExecutionException error) {
      if (error.getCause() instanceof RuntimeException causa) throw causa;
      throw new IllegalStateException(error.getCause());
    }
  }
}
