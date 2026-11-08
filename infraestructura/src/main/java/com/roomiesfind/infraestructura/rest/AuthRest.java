package com.roomiesfind.infraestructura.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomiesfind.contratos.perfiles.DatosPerfil;
import com.roomiesfind.contratos.perfiles.Foto;
import com.roomiesfind.contratos.usuarios.AutenticarRequest;
import com.roomiesfind.contratos.usuarios.ObtenerUsuariosRequest;
import com.roomiesfind.contratos.usuarios.SesionResponse;
import com.roomiesfind.contratos.usuarios.Usuario;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.orquestacion.RegistroOrquestador;
import com.roomiesfind.infraestructura.seguridad.AutenticacionInterceptor;
import com.roomiesfind.infraestructura.seguridad.RequiereAuth;
import com.roomiesfind.infraestructura.seguridad.Suspensiones;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
  private final Suspensiones suspensiones;

  public AuthRest(RegistroOrquestador registro, ClientesGrpc clientes, Suspensiones suspensiones) {
    this.registro = registro;
    this.clientes = clientes;
    this.suspensiones = suspensiones;
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
    SesionResponse sesion = clientes.usuarios().autenticar(AutenticarRequest.newBuilder()
        .setEmail(datos.path("email").asText(""))
        .setPassword(datos.path("password").asText(""))
        .build());
    // Se comprueba después de la contraseña, para no revelar qué correos están suspendidos.
    if (suspensiones.estaSuspendida(sesion.getUsuario().getId())) {
      throw new ErrorRest(HttpStatus.FORBIDDEN, AutenticacionInterceptor.CUENTA_SUSPENDIDA);
    }
    return aRespuesta(sesion);
  }

  /** Datos actuales de quien tiene la sesión, para que el frontend sepa su rol aunque haya cambiado. */
  @RequiereAuth
  @GetMapping("/sesion")
  Map<String, Object> sesion(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId) {
    Usuario usuario = clientes.usuarios()
        .obtenerUsuarios(ObtenerUsuariosRequest.newBuilder().addIds(usuarioId).build())
        .getUsuariosList().stream().findFirst()
        .orElseThrow(() -> new ErrorRest(HttpStatus.UNAUTHORIZED, "No autenticado."));
    return aUsuario(usuario);
  }

  private static Map<String, Object> aRespuesta(SesionResponse sesion) {
    Map<String, Object> respuesta = new LinkedHashMap<>();
    respuesta.put("token", sesion.getToken());
    respuesta.put("usuario", aUsuario(sesion.getUsuario()));
    return respuesta;
  }

  private static Map<String, Object> aUsuario(Usuario usuario) {
    Map<String, Object> datos = new LinkedHashMap<>();
    datos.put("id", usuario.getId());
    datos.put("nombre", usuario.getNombre());
    datos.put("email", usuario.getEmail());
    datos.put("rol", usuario.getRol().isEmpty() ? "usuario" : usuario.getRol());
    return datos;
  }
}
