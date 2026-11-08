package com.roomiesfind.infraestructura.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Empty;
import com.roomiesfind.contratos.moderacion.CuentaReportada;
import com.roomiesfind.contratos.moderacion.ListarReportesRequest;
import com.roomiesfind.contratos.moderacion.Reporte;
import com.roomiesfind.contratos.moderacion.ResolverReportesRequest;
import com.roomiesfind.contratos.moderacion.ResumenModeracion;
import com.roomiesfind.contratos.moderacion.SuspenderRequest;
import com.roomiesfind.contratos.moderacion.Suspension;
import com.roomiesfind.contratos.moderacion.UsuarioRequest;
import com.roomiesfind.infraestructura.chat.ChatWebSocket;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.orquestacion.ConsultaPerfiles;
import com.roomiesfind.infraestructura.seguridad.AutenticacionInterceptor;
import com.roomiesfind.infraestructura.seguridad.RequiereAdmin;
import com.roomiesfind.infraestructura.seguridad.Suspensiones;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panel de administración. Solo entra quien tiene el rol de administrador.
 * Lo guarda moderación y aquí se le pone nombre y foto a cada cuenta.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminRest {

  private final ClientesGrpc clientes;
  private final ConsultaPerfiles consulta;
  private final Suspensiones suspensiones;
  private final ChatWebSocket chat;

  public AdminRest(ClientesGrpc clientes, ConsultaPerfiles consulta, Suspensiones suspensiones, ChatWebSocket chat) {
    this.clientes = clientes;
    this.consulta = consulta;
    this.suspensiones = suspensiones;
    this.chat = chat;
  }

  @RequiereAdmin
  @GetMapping("/resumen")
  Map<String, Object> resumen() {
    ResumenModeracion resumen = clientes.moderacion().obtenerResumen(Empty.getDefaultInstance());
    Map<String, Object> respuesta = new LinkedHashMap<>();
    respuesta.put("cuentasPorRevisar", resumen.getCuentasPorRevisar());
    respuesta.put("reportesPendientes", resumen.getReportesPendientes());
    respuesta.put("alertasActivas", resumen.getAlertasActivas());
    respuesta.put("cuentasSuspendidas", resumen.getCuentasSuspendidas());
    return respuesta;
  }

  /** @param estado "pendientes" (por defecto) o "todos" */
  @RequiereAdmin
  @GetMapping("/reportes")
  List<Map<String, Object>> reportes(@RequestParam(required = false) String estado) {
    List<CuentaReportada> cuentas = clientes.moderacion()
        .listarReportes(ListarReportesRequest.newBuilder().setSoloPendientes(!"todos".equals(estado)).build())
        .getCuentasList();
    Set<Integer> ids = new LinkedHashSet<>();
    for (CuentaReportada cuenta : cuentas) {
      ids.add(cuenta.getUsuarioId());
      cuenta.getReportesList().forEach((reporte) -> ids.add(reporte.getReportanteId()));
    }
    Map<Integer, Map<String, Object>> fichas = consulta.fichas(List.copyOf(ids));

    List<Map<String, Object>> respuesta = new ArrayList<>();
    for (CuentaReportada cuenta : cuentas) {
      Map<String, Object> persona = fichas.get(cuenta.getUsuarioId());
      if (persona == null) continue;
      List<Map<String, Object>> reportes = new ArrayList<>();
      for (Reporte reporte : cuenta.getReportesList()) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", reporte.getId());
        fila.put("motivo", reporte.getMotivo());
        fila.put("detalle", reporte.hasDetalle() ? reporte.getDetalle() : null);
        fila.put("estado", reporte.getEstado());
        fila.put("creadoEn", reporte.getCreadoEn());
        fila.put("reportante", resumenDe(fichas.get(reporte.getReportanteId())));
        reportes.add(fila);
      }
      Map<String, Object> fila = new LinkedHashMap<>();
      fila.put("persona", persona);
      fila.put("alerta", cuenta.getAlerta());
      fila.put("suspendida", cuenta.getSuspendida());
      fila.put("reportes", reportes);
      respuesta.add(fila);
    }
    return respuesta;
  }

  @RequiereAdmin
  @PostMapping("/cuentas/{usuarioId}/descartar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void descartar(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int adminId, @PathVariable String usuarioId) {
    resolver(adminId, leerId(usuarioId), "descartar");
  }

  @RequiereAdmin
  @PostMapping("/cuentas/{usuarioId}/mantener")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void mantener(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int adminId, @PathVariable String usuarioId) {
    resolver(adminId, leerId(usuarioId), "mantener");
  }

  @RequiereAdmin
  @PostMapping("/cuentas/{usuarioId}/suspension")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void suspender(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int adminId, @PathVariable String usuarioId,
      @RequestBody(required = false) JsonNode cuerpo) {
    int id = leerId(usuarioId);
    Map<String, Object> persona = consulta.fichas(List.of(id)).get(id);
    if (persona == null) throw new ErrorRest(HttpStatus.NOT_FOUND, "La persona no existe.");
    if ("admin".equals(persona.get("rol")) && id != adminId) {
      throw new ErrorRest(HttpStatus.BAD_REQUEST, "No puedes suspender a un administrador.");
    }
    clientes.moderacion().suspender(SuspenderRequest.newBuilder()
        .setAdminId(adminId)
        .setUsuarioId(id)
        .setMotivo(Json.objetoOVacio(cuerpo).path("motivo").asText(""))
        .build());
    suspensiones.olvidar(id);
    chat.cerrarConexionesDe(id);
  }

  @RequiereAdmin
  @DeleteMapping("/cuentas/{usuarioId}/suspension")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void reactivar(@PathVariable String usuarioId) {
    int id = leerId(usuarioId);
    clientes.moderacion().reactivar(UsuarioRequest.newBuilder().setUsuarioId(id).build());
    suspensiones.olvidar(id);
  }

  @RequiereAdmin
  @GetMapping("/suspendidas")
  List<Map<String, Object>> suspendidas() {
    List<Suspension> lista = clientes.moderacion().listarSuspendidas(Empty.getDefaultInstance()).getSuspensionesList();
    Set<Integer> ids = new LinkedHashSet<>();
    for (Suspension suspension : lista) {
      ids.add(suspension.getUsuarioId());
      if (suspension.hasSuspendidaPor()) ids.add(suspension.getSuspendidaPor());
    }
    Map<Integer, Map<String, Object>> fichas = consulta.fichas(List.copyOf(ids));
    List<Map<String, Object>> respuesta = new ArrayList<>();
    for (Suspension suspension : lista) {
      Map<String, Object> persona = fichas.get(suspension.getUsuarioId());
      if (persona == null) continue;
      Map<String, Object> fila = new LinkedHashMap<>();
      fila.put("persona", persona);
      fila.put("motivo", suspension.getMotivo());
      fila.put("creadaEn", suspension.getCreadaEn());
      fila.put("suspendidaPor", suspension.hasSuspendidaPor() ? resumenDe(fichas.get(suspension.getSuspendidaPor())) : null);
      respuesta.add(fila);
    }
    return respuesta;
  }

  private void resolver(int adminId, int usuarioId, String decision) {
    clientes.moderacion().resolverReportes(ResolverReportesRequest.newBuilder()
        .setAdminId(adminId).setUsuarioId(usuarioId).setDecision(decision).build());
  }

  private static Map<String, Object> resumenDe(Map<String, Object> ficha) {
    if (ficha == null) return null;
    Map<String, Object> resumen = new LinkedHashMap<>();
    resumen.put("id", ficha.get("id"));
    resumen.put("nombre", ficha.get("nombre"));
    return resumen;
  }

  private static int leerId(String texto) {
    try {
      int id = Integer.parseInt(texto);
      if (id > 0) return id;
    } catch (NumberFormatException error) {
      // Se responde abajo como solicitud inválida.
    }
    throw new ErrorRest(HttpStatus.BAD_REQUEST, "La solicitud no es válida.");
  }
}
