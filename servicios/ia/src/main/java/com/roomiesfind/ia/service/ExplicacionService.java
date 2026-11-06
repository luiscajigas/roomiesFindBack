package com.roomiesfind.ia.service;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.ia.dto.SolicitudTextoDto;
import com.roomiesfind.ia.model.ProveedorIa;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Redacta con el primer proveedor disponible, en orden: Claude y luego Gemini de respaldo. */
@Service
public class ExplicacionService {

  private static final Logger log = LoggerFactory.getLogger(ExplicacionService.class);

  private final List<ProveedorIa> proveedores;

  public ExplicacionService(List<ProveedorIa> proveedores) {
    this.proveedores = proveedores;
  }

  public String generar(SolicitudTextoDto solicitud) {
    return RecorteTexto.hastaUltimaOracionCompleta(conPrimerProveedor(PromptExplicacion.construir(solicitud)));
  }

  public String traducir(String texto, String idioma) {
    return conPrimerProveedor(PromptTraduccion.construir(texto, idioma));
  }

  private String conPrimerProveedor(String prompt) {
    RuntimeException ultimoError = null;

    for (ProveedorIa proveedor : proveedores) {
      if (!proveedor.estaConfigurado()) continue;
      try {
        return proveedor.generar(prompt);
      } catch (RuntimeException error) {
        ultimoError = error;
        log.warn("Falló la IA con {}: {}", proveedor.nombre(), error.getMessage());
      }
    }
    if (ultimoError == null) {
      throw ErrorNegocio.noDisponible("Configura ANTHROPIC_API_KEY o GEMINI_API_KEY en el servidor.");
    }
    throw ultimoError;
  }
}
