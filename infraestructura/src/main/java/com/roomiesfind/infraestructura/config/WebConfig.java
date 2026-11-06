package com.roomiesfind.infraestructura.config;

import com.roomiesfind.comun.Entorno;
import com.roomiesfind.infraestructura.seguridad.AutenticacionInterceptor;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS, autenticación y fotos antiguas guardadas en disco. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private static final String VERCEL = ".vercel.app";

  private final AutenticacionInterceptor autenticacion;

  public WebConfig(AutenticacionInterceptor autenticacion) {
    this.autenticacion = autenticacion;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registro) {
    registro.addInterceptor(autenticacion);
  }

  @Override
  public void addCorsMappings(CorsRegistry registro) {
    String frontend = Entorno.opcional("FRONTEND_URL", Entorno.esProduccion() ? "" : "http://localhost:4300");
    registro.addMapping("/**")
        .allowedOriginPatterns(origenesPermitidos(frontend).toArray(String[]::new))
        .allowedMethods("GET", "HEAD", "PUT", "PATCH", "POST", "DELETE")
        .allowedHeaders("*");
  }

  /** Además de los orígenes configurados, acepta las vistas previas de Vercel del mismo proyecto. */
  public static List<String> origenesPermitidos(String frontendUrl) {
    List<String> origenes = new ArrayList<>(Arrays.stream(frontendUrl.split(","))
        .map(String::trim).filter((origen) -> !origen.isEmpty()).toList());
    origenes.stream()
        .map(WebConfig::proyectoVercel)
        .filter((proyecto) -> proyecto != null)
        .findFirst()
        .ifPresent((proyecto) -> origenes.add("https://" + proyecto + "-*" + VERCEL));
    return origenes;
  }

  private static String proyectoVercel(String origen) {
    try {
      String host = URI.create(origen).getHost();
      return host != null && host.endsWith(VERCEL) ? host.substring(0, host.length() - VERCEL.length()) : null;
    } catch (IllegalArgumentException error) {
      return null;
    }
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registro) {
    Path carpeta = Path.of(Entorno.opcional("UPLOADS_DIR", "uploads")).toAbsolutePath();
    registro.addResourceHandler("/uploads/**").addResourceLocations(carpeta.toUri().toString());
  }
}
