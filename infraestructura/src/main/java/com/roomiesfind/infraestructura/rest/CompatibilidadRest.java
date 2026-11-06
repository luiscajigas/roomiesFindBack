package com.roomiesfind.infraestructura.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomiesfind.contratos.compatibilidad.ExplicarRequest;
import com.roomiesfind.contratos.compatibilidad.ExplicarResponse;
import com.roomiesfind.contratos.compatibilidad.RegistrarRecomendacionRequest;
import com.roomiesfind.contratos.compatibilidad.TraducirRequest;
import com.roomiesfind.contratos.perfiles.ObtenerPerfilRequest;
import com.roomiesfind.contratos.perfiles.Perfil;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.seguridad.AutenticacionInterceptor;
import com.roomiesfind.infraestructura.seguridad.RequiereAuth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Historial de puntajes y explicaciones. Recibe REST del frontend y llama al microservicio por gRPC. */
@RestController
@RequestMapping({"/api/compatibilidad", "/api/ia"})
public class CompatibilidadRest {

  private final ClientesGrpc clientes;

  public CompatibilidadRest(ClientesGrpc clientes) {
    this.clientes = clientes;
  }

  @RequiereAuth
  @PostMapping("/registrar")
  @ResponseStatus(HttpStatus.CREATED)
  Map<String, Boolean> registrar(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId,
      @RequestBody(required = false) JsonNode cuerpo) {
    JsonNode datos = Json.objetoOVacio(cuerpo);
    RegistrarRecomendacionRequest.Builder peticion = RegistrarRecomendacionRequest.newBuilder().setUsuarioId(usuarioId);
    if (Json.tieneValor(datos, "candidatoId")) peticion.setCandidatoId(Json.comoNumero(datos.get("candidatoId")));
    if (Json.tieneValor(datos, "score")) peticion.setScore(Json.comoNumero(datos.get("score")));
    clientes.compatibilidad().registrarRecomendacion(peticion.build());
    return Map.of("registrado", true);
  }

  /**
   * Traduce la descripción de una persona. Recibe a quién y no el texto:
   * así la ruta solo traduce perfiles reales y no sirve de traductor gratuito.
   */
  @RequiereAuth
  @PostMapping("/traducir-descripcion")
  Map<String, String> traducirDescripcion(@RequestBody(required = false) JsonNode cuerpo) {
    JsonNode datos = Json.objetoOVacio(cuerpo);
    double id = Json.tieneValor(datos, "usuarioId") ? Json.comoNumero(datos.get("usuarioId")) : Double.NaN;
    if (!(id > 0 && id <= Integer.MAX_VALUE && id == Math.rint(id))) {
      throw new ErrorRest(HttpStatus.BAD_REQUEST, "La solicitud no es válida.");
    }
    Perfil perfil = clientes.perfiles().obtenerPerfil(ObtenerPerfilRequest.newBuilder().setUsuarioId((int) id).build());
    if (!perfil.hasDescripcion() || perfil.getDescripcion().isBlank()) {
      throw new ErrorRest(HttpStatus.NOT_FOUND, "Perfil no encontrado.");
    }
    String traduccion = clientes.compatibilidad().traducir(TraducirRequest.newBuilder()
        .setTexto(perfil.getDescripcion())
        .setIdioma(datos.path("idioma").asText(""))
        .build()).getTexto();
    return Map.of("descripcion", traduccion);
  }

  @RequiereAuth
  @PostMapping("/explicar")
  Map<String, Object> explicar(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId,
      @RequestBody(required = false) JsonNode cuerpo) {
    JsonNode datos = Json.objetoOVacio(cuerpo);
    List<String> coincidencias = Json.comoListaDeTexto(datos.get("coincidencias"));
    List<String> conflictos = Json.comoListaDeTexto(datos.get("conflictos"));

    ExplicarResponse explicacion = clientes.compatibilidad().explicar(ExplicarRequest.newBuilder()
        .setUsuarioId(usuarioId)
        .setCandidatoId(Json.tieneValor(datos, "candidatoId") ? Json.comoNumero(datos.get("candidatoId")) : Double.NaN)
        .setScore(Json.tieneValor(datos, "score") ? Json.comoNumero(datos.get("score")) : Double.NaN)
        .addAllCoincidencias(coincidencias)
        .addAllConflictos(conflictos)
        .setIdioma(datos.path("idioma").asText("es"))
        .build());

    Map<String, Object> respuesta = new LinkedHashMap<>();
    respuesta.put("id", explicacion.getId());
    respuesta.put("estado", explicacion.getEstado());
    respuesta.put("explicacion", explicacion.getExplicacion());
    respuesta.put("score", explicacion.getScore());
    respuesta.put("coincidencias", coincidencias);
    respuesta.put("conflictos", conflictos);
    respuesta.put("desdeCache", explicacion.getDesdeCache());
    return respuesta;
  }
}
