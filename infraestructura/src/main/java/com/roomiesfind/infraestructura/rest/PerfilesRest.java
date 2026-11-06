package com.roomiesfind.infraestructura.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomiesfind.contratos.perfiles.ActualizarPerfilRequest;
import com.roomiesfind.contratos.perfiles.DatosPerfil;
import com.roomiesfind.contratos.perfiles.Foto;
import com.roomiesfind.contratos.perfiles.GuardarFotoRequest;
import com.roomiesfind.contratos.perfiles.ObtenerFotoRequest;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.orquestacion.ConsultaPerfiles;
import com.roomiesfind.infraestructura.seguridad.AutenticacionInterceptor;
import com.roomiesfind.infraestructura.seguridad.RequiereAuth;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Perfiles, candidatos y fotos. Recibe REST del frontend y llama a los microservicios por gRPC. */
@RestController
@RequestMapping("/api/perfiles")
public class PerfilesRest {

  private final ClientesGrpc clientes;
  private final ConsultaPerfiles consulta;

  public PerfilesRest(ClientesGrpc clientes, ConsultaPerfiles consulta) {
    this.clientes = clientes;
    this.consulta = consulta;
  }

  @GetMapping("/{usuarioId}/foto")
  ResponseEntity<byte[]> foto(@PathVariable String usuarioId) {
    Foto foto = clientes.perfiles().obtenerFoto(
        ObtenerFotoRequest.newBuilder().setUsuarioId(leerId(usuarioId)).build());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(foto.getTipoMime()))
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body(foto.getContenido().toByteArray());
  }

  @RequiereAuth
  @GetMapping("/me")
  Map<String, Object> miPerfil(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId) {
    return consulta.miPerfil(usuarioId);
  }

  @RequiereAuth
  @PutMapping("/me/foto")
  Map<String, String> guardarFoto(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId,
      @RequestParam(value = "foto", required = false) MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ErrorRest(HttpStatus.BAD_REQUEST, "Selecciona una foto para continuar.");
    }
    String ruta = clientes.perfiles().guardarFoto(GuardarFotoRequest.newBuilder()
        .setUsuarioId(usuarioId).setFoto(Archivos.aFoto(archivo)).build()).getFotoUrl();
    return Map.of("foto_url", ruta);
  }

  @RequiereAuth
  @PutMapping("/me")
  Map<String, Object> actualizar(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId,
      @RequestBody(required = false) JsonNode cuerpo) {
    DatosPerfil datos = leerCambios(Json.objetoOVacio(cuerpo));
    return ConsultaPerfiles.aMapa(clientes.perfiles().actualizarPerfil(
        ActualizarPerfilRequest.newBuilder().setUsuarioId(usuarioId).setDatos(datos).build()));
  }

  @RequiereAuth
  @GetMapping("/candidatos")
  List<Map<String, Object>> candidatos(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId) {
    return consulta.candidatos(usuarioId);
  }

  @RequiereAuth
  @GetMapping("/{usuarioId}")
  Map<String, Object> persona(@PathVariable String usuarioId) {
    return consulta.personas(List.of(leerId(usuarioId))).stream().findFirst()
        .orElseThrow(() -> new ErrorRest(HttpStatus.NOT_FOUND, "Perfil no encontrado."));
  }

  private static int leerId(String texto) {
    try {
      BigDecimal id = new BigDecimal(texto);
      if (id.signum() > 0 && id.stripTrailingZeros().scale() <= 0) {
        // Un número válido pero enorme simplemente no existe.
        return id.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0 ? Integer.MAX_VALUE : id.intValueExact();
      }
    } catch (NumberFormatException error) {
      // Se responde abajo como identificador inválido.
    }
    throw new ErrorRest(HttpStatus.BAD_REQUEST, "Identificador de perfil inválido.");
  }

  private static DatosPerfil leerCambios(JsonNode cuerpo) {
    DatosPerfil.Builder datos = DatosPerfil.newBuilder();
    if (Json.tieneValor(cuerpo, "presupuesto")) datos.setPresupuesto(cuerpo.get("presupuesto").asText());
    if (Json.tieneValor(cuerpo, "zona")) datos.setZona(cuerpo.get("zona").asText());
    if (Json.tieneValor(cuerpo, "horario")) datos.setHorario(cuerpo.get("horario").asText());
    if (Json.tieneValor(cuerpo, "limpieza")) datos.setLimpieza(cuerpo.get("limpieza").asText());
    if (Json.tieneValor(cuerpo, "tolerancia_ruido")) datos.setToleranciaRuido(cuerpo.get("tolerancia_ruido").asText());
    if (Json.tieneValor(cuerpo, "frecuencia_visitas")) datos.setFrecuenciaVisitas(cuerpo.get("frecuencia_visitas").asText());
    if (Json.tieneValor(cuerpo, "tiene_mascotas")) datos.setTieneMascotas(Json.comoBooleano(cuerpo.get("tiene_mascotas")));
    if (Json.tieneValor(cuerpo, "acepta_mascotas")) datos.setAceptaMascotas(Json.comoBooleano(cuerpo.get("acepta_mascotas")));
    if (Json.tieneValor(cuerpo, "descripcion")) datos.setDescripcion(cuerpo.get("descripcion").asText());
    if (Json.tieneValor(cuerpo, "foto_url")) datos.setFotoUrl(cuerpo.get("foto_url").asText());
    return datos.build();
  }
}
