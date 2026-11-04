package com.roomiesfind.infraestructura.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomiesfind.contratos.perfiles.DatosPerfil;
import com.roomiesfind.contratos.perfiles.Foto;
import com.roomiesfind.contratos.usuarios.AutenticarRequest;
import com.roomiesfind.contratos.usuarios.SesionResponse;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.orquestacion.RegistroOrquestador;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Registro e inicio de sesión. Recibe REST del frontend y responde con lo que dan los microservicios por gRPC. */
@RestController
@RequestMapping("/api/auth")
public class AuthRest {

  private final RegistroOrquestador registro;
  private final ClientesGrpc clientes;

  public AuthRest(RegistroOrquestador registro, ClientesGrpc clientes) {
    this.registro = registro;
    this.clientes = clientes;
  }

  @PostMapping("/registro")
  @ResponseStatus(HttpStatus.CREATED)
  Map<String, Object> registrar(@RequestParam Map<String, String> campos,
      @RequestParam(value = "foto", required = false) MultipartFile archivo) {
    for (String obligatorio : new String[] {"nombre", "email", "password", "presupuesto", "zona", "horario"}) {
      String valor = campos.get(obligatorio);
      if (valor == null || valor.isEmpty()) {
        throw new ErrorRest(HttpStatus.BAD_REQUEST, "Faltan campos obligatorios del registro o del perfil.");
      }
    }
    DatosPerfil.Builder datos = DatosPerfil.newBuilder()
        .setPresupuesto(campos.get("presupuesto"))
        .setZona(campos.get("zona"))
        .setHorario(campos.get("horario"))
        .setTieneMascotas("true".equals(campos.get("tiene_mascotas")))
        .setAceptaMascotas(!campos.containsKey("acepta_mascotas") || "true".equals(campos.get("acepta_mascotas")));
    if (campos.containsKey("limpieza")) datos.setLimpieza(campos.get("limpieza"));
    if (campos.containsKey("tolerancia_ruido")) datos.setToleranciaRuido(campos.get("tolerancia_ruido"));
    if (campos.containsKey("frecuencia_visitas")) datos.setFrecuenciaVisitas(campos.get("frecuencia_visitas"));
    if (campos.containsKey("descripcion")) datos.setDescripcion(campos.get("descripcion"));

    Foto foto = archivo == null || archivo.isEmpty() ? null : Archivos.aFoto(archivo);
    SesionResponse sesion = registro.registrar(
        campos.get("nombre"), campos.get("email"), campos.get("password"), datos.build(), foto);
    return aRespuesta(sesion);
  }

  @PostMapping("/login")
  Map<String, Object> login(@RequestBody(required = false) JsonNode cuerpo) {
    JsonNode datos = Json.objetoOVacio(cuerpo);
    return aRespuesta(clientes.usuarios().autenticar(AutenticarRequest.newBuilder()
        .setEmail(datos.path("email").asText(""))
        .setPassword(datos.path("password").asText(""))
        .build()));
  }

  private static Map<String, Object> aRespuesta(SesionResponse sesion) {
    Map<String, Object> usuario = new LinkedHashMap<>();
    usuario.put("id", sesion.getUsuario().getId());
    usuario.put("nombre", sesion.getUsuario().getNombre());
    usuario.put("email", sesion.getUsuario().getEmail());

    Map<String, Object> respuesta = new LinkedHashMap<>();
    respuesta.put("token", sesion.getToken());
    respuesta.put("usuario", usuario);
    return respuesta;
  }
}
