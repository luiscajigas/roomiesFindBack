package com.roomiesfind.infraestructura.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomiesfind.contratos.perfiles.ActualizarPerfilRequest;
import com.roomiesfind.contratos.perfiles.BuscarCandidatosRequest;
import com.roomiesfind.contratos.perfiles.DatosPerfil;
import com.roomiesfind.contratos.perfiles.Foto;
import com.roomiesfind.contratos.perfiles.GuardarFotoRequest;
import com.roomiesfind.contratos.perfiles.ObtenerFotoRequest;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.contrato.OtrasRespuestas.FotoGuardada;
import com.roomiesfind.infraestructura.contrato.PaginaCandidatosRespuesta;
import com.roomiesfind.infraestructura.contrato.PersonaRespuesta;
import com.roomiesfind.infraestructura.orquestacion.ConsultaPerfiles;
import com.roomiesfind.infraestructura.seguridad.AutenticacionInterceptor;
import com.roomiesfind.infraestructura.seguridad.RequiereAuth;
import java.util.List;
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
        ObtenerFotoRequest.newBuilder().setUsuarioId(Parametros.idTolerante(usuarioId)).build());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(foto.getTipoMime()))
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body(foto.getContenido().toByteArray());
  }

  @RequiereAuth
  @GetMapping("/me")
  PersonaRespuesta miPerfil(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId) {
    return consulta.miPerfil(usuarioId);
  }

  @RequiereAuth
  @PutMapping("/me/foto")
  FotoGuardada guardarFoto(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId,
      @RequestParam(value = "foto", required = false) MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ErrorRest(HttpStatus.BAD_REQUEST, "Selecciona una foto para continuar.");
    }
    String ruta = clientes.perfiles().guardarFoto(GuardarFotoRequest.newBuilder()
        .setUsuarioId(usuarioId).setFoto(Archivos.aFoto(archivo)).build()).getFotoUrl();
    return new FotoGuardada(ruta);
  }

  @RequiereAuth
  @PutMapping("/me")
  PersonaRespuesta actualizar(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId,
      @RequestBody(required = false) JsonNode cuerpo) {
    DatosPerfil datos = leerCambios(Json.objetoOVacio(cuerpo));
    clientes.perfiles().actualizarPerfil(ActualizarPerfilRequest.newBuilder().setUsuarioId(usuarioId).setDatos(datos).build());
    return consulta.miPerfil(usuarioId);
  }

  @RequiereAuth
  @GetMapping("/{usuarioId}")
  PersonaRespuesta persona(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int yo,
      @PathVariable String usuarioId) {
    int id = Parametros.idTolerante(usuarioId);
    // Para quien bloqueó o fue bloqueado, la otra persona no existe.
    if (consulta.excluidos(yo).contains(id)) throw new ErrorRest(HttpStatus.NOT_FOUND, "Perfil no encontrado.");
    return consulta.personas(List.of(id)).stream().findFirst()
        .orElseThrow(() -> new ErrorRest(HttpStatus.NOT_FOUND, "Perfil no encontrado."));
  }

  /** Una página de personas con los filtros que lleguen. Lo que no llega no se filtra. */
  @RequiereAuth
  @GetMapping("/buscar")
  PaginaCandidatosRespuesta buscar(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId,
      @RequestParam(required = false) String pagina,
      @RequestParam(required = false) String tamano,
      @RequestParam(required = false) String orden,
      @RequestParam(value = "zona", required = false) List<String> zonas,
      @RequestParam(required = false) String horario,
      @RequestParam(required = false) String presupuestoMaximo,
      @RequestParam(required = false) String mascotas,
      @RequestParam(required = false) String puntajeMinimo) {
    BuscarCandidatosRequest.Builder peticion = BuscarCandidatosRequest.newBuilder();
    if (Parametros.tieneTexto(pagina)) peticion.setPagina(Parametros.entero(pagina));
    if (Parametros.tieneTexto(tamano)) peticion.setTamano(Parametros.entero(tamano));
    if (Parametros.tieneTexto(orden)) peticion.setOrden(orden);
    if (zonas != null) peticion.addAllZonas(zonas.stream().filter(Parametros::tieneTexto).toList());
    if (Parametros.tieneTexto(horario)) peticion.setHorario(horario);
    if (Parametros.tieneTexto(presupuestoMaximo)) peticion.setPresupuestoMaximo(Parametros.entero(presupuestoMaximo));
    if (Parametros.tieneTexto(mascotas)) peticion.setMascotas(mascotas);
    if (Parametros.tieneTexto(puntajeMinimo)) peticion.setPuntajeMinimo(Parametros.entero(puntajeMinimo));
    return consulta.buscar(usuarioId, peticion);
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
