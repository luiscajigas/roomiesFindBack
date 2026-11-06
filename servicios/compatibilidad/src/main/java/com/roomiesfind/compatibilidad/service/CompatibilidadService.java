package com.roomiesfind.compatibilidad.service;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.compatibilidad.cliente.IaCliente;
import com.roomiesfind.compatibilidad.cliente.UsuariosCliente;
import com.roomiesfind.compatibilidad.dto.ExplicacionDto;
import com.roomiesfind.compatibilidad.dto.SolicitudExplicacionDto;
import com.roomiesfind.compatibilidad.model.Explicacion;
import com.roomiesfind.compatibilidad.repository.ExplicacionRepository;
import com.roomiesfind.compatibilidad.repository.RecomendacionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Guarda el historial de puntajes y pide las explicaciones. La IA nunca decide
 * quién es compatible: solo interpreta el puntaje que calculó el frontend.
 */
@Service
public class CompatibilidadService {

  private static final Logger log = LoggerFactory.getLogger(CompatibilidadService.class);
  private static final String ESTADO_GENERADO = "generado";

  private final RecomendacionRepository recomendaciones;
  private final ExplicacionRepository explicaciones;
  private final UsuariosCliente usuarios;
  private final IaCliente ia;

  public CompatibilidadService(RecomendacionRepository recomendaciones, ExplicacionRepository explicaciones,
      UsuariosCliente usuarios, IaCliente ia) {
    this.recomendaciones = recomendaciones;
    this.explicaciones = explicaciones;
    this.usuarios = usuarios;
    this.ia = ia;
  }

  public void registrar(int usuarioId, int candidatoId, int score) {
    recomendaciones.guardar(usuarioId, candidatoId, score);
  }

  public ExplicacionDto explicar(SolicitudExplicacionDto solicitud) {
    int usuarioId = solicitud.usuarioId();
    int candidatoId = (int) solicitud.candidatoId();
    int score = (int) solicitud.score();
    String idioma = "en".equals(solicitud.idioma()) ? "en" : "es";

    try {
      Map<Integer, String> nombres = usuarios.nombres(List.of(usuarioId, candidatoId));
      if (!nombres.containsKey(candidatoId)) {
        throw ErrorNegocio.noEncontrado("El perfil del candidato no existe.");
      }

      // Para no repetir llamadas a la IA, se reutiliza lo ya generado para esta pareja, puntaje e idioma.
      Optional<Explicacion> guardada = explicaciones.buscarGenerada(usuarioId, candidatoId, score, idioma);
      if (guardada.isPresent()) {
        return new ExplicacionDto(guardada.get().id(), ESTADO_GENERADO, guardada.get().texto(), score, true);
      }

      int id = explicaciones.crearPendiente(usuarioId, candidatoId, score, idioma);
      String texto = ia.generar(
          nombres.getOrDefault(usuarioId, "la persona usuaria"), nombres.get(candidatoId), score,
          solicitud.coincidencias(), solicitud.conflictos(), idioma);
      explicaciones.marcarGenerada(id, texto);
      return new ExplicacionDto(id, ESTADO_GENERADO, texto, score, false);
    } catch (ErrorNegocio error) {
      throw error;
    } catch (RuntimeException error) {
      log.error("No se pudo generar la explicación con IA: {}", error.getMessage());
      throw ErrorNegocio.noDisponible(
          "La explicación con IA no está disponible en este momento. Inténtalo de nuevo en unos segundos.");
    }
  }
}
