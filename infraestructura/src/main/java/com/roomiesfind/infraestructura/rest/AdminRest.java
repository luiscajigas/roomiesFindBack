package com.roomiesfind.infraestructura.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Empty;
import com.roomiesfind.contratos.moderacion.CuentaReportada;
import com.roomiesfind.contratos.moderacion.ListarReportesRequest;
import com.roomiesfind.contratos.moderacion.ResolverReportesRequest;
import com.roomiesfind.contratos.moderacion.ResumenModeracion;
import com.roomiesfind.contratos.moderacion.SuspenderRequest;
import com.roomiesfind.contratos.moderacion.Suspension;
import com.roomiesfind.contratos.moderacion.UsuarioRequest;
import com.roomiesfind.infraestructura.chat.ChatWebSocket;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.contrato.AdminRespuestas;
import com.roomiesfind.infraestructura.contrato.AdminRespuestas.Ficha;
import com.roomiesfind.infraestructura.contrato.AdminRespuestas.PersonaCorta;
import com.roomiesfind.infraestructura.contrato.AdminRespuestas.Resumen;
import com.roomiesfind.infraestructura.contrato.Roles;
import com.roomiesfind.infraestructura.orquestacion.ConsultaPerfiles;
import com.roomiesfind.infraestructura.seguridad.AutenticacionInterceptor;
import com.roomiesfind.infraestructura.seguridad.RequiereAdmin;
import com.roomiesfind.infraestructura.seguridad.Suspensiones;
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
  Resumen resumen() {
    ResumenModeracion resumen = clientes.moderacion().obtenerResumen(Empty.getDefaultInstance());
    return new Resumen(resumen.getCuentasPorRevisar(), resumen.getReportesPendientes(), resumen.getAlertasActivas(),
        resumen.getCuentasSuspendidas());
  }

  /** @param estado "pendientes" (por defecto) o "todos" */
  @RequiereAdmin
  @GetMapping("/reportes")
  List<AdminRespuestas.CuentaReportada> reportes(@RequestParam(required = false) String estado) {
    List<CuentaReportada> cuentas = clientes.moderacion()
        .listarReportes(ListarReportesRequest.newBuilder().setSoloPendientes(!"todos".equals(estado)).build())
        .getCuentasList();
    Set<Integer> ids = new LinkedHashSet<>();
    for (CuentaReportada cuenta : cuentas) {
      ids.add(cuenta.getUsuarioId());
      cuenta.getReportesList().forEach((reporte) -> ids.add(reporte.getReportanteId()));
    }
    Map<Integer, Ficha> fichas = consulta.fichas(List.copyOf(ids));

    return cuentas.stream()
        // Una cuenta borrada ya no tiene nada que revisar.
        .filter((cuenta) -> fichas.containsKey(cuenta.getUsuarioId()))
        .map((cuenta) -> new AdminRespuestas.CuentaReportada(
            fichas.get(cuenta.getUsuarioId()),
            cuenta.getAlerta(),
            cuenta.getSuspendida(),
            cuenta.getReportesList().stream()
                .map((reporte) -> new AdminRespuestas.Reporte(reporte.getId(), reporte.getMotivo(),
                    reporte.hasDetalle() ? reporte.getDetalle() : null, reporte.getEstado(), reporte.getCreadoEn(),
                    corta(fichas.get(reporte.getReportanteId()))))
                .toList()))
        .toList();
  }

  @RequiereAdmin
  @PostMapping("/cuentas/{usuarioId}/descartar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void descartar(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int adminId, @PathVariable String usuarioId) {
    resolver(adminId, Parametros.id(usuarioId), "descartar");
  }

  @RequiereAdmin
  @PostMapping("/cuentas/{usuarioId}/mantener")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void mantener(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int adminId, @PathVariable String usuarioId) {
    resolver(adminId, Parametros.id(usuarioId), "mantener");
  }

  @RequiereAdmin
  @PostMapping("/cuentas/{usuarioId}/suspension")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void suspender(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int adminId, @PathVariable String usuarioId,
      @RequestBody(required = false) JsonNode cuerpo) {
    int id = Parametros.id(usuarioId);
    Ficha persona = consulta.fichas(List.of(id)).get(id);
    if (persona == null) throw new ErrorRest(HttpStatus.NOT_FOUND, "La persona no existe.");
    if (Roles.ADMIN.equals(persona.rol()) && id != adminId) {
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
    int id = Parametros.id(usuarioId);
    clientes.moderacion().reactivar(UsuarioRequest.newBuilder().setUsuarioId(id).build());
    suspensiones.olvidar(id);
  }

  @RequiereAdmin
  @GetMapping("/suspendidas")
  List<AdminRespuestas.Suspension> suspendidas() {
    List<Suspension> lista = clientes.moderacion().listarSuspendidas(Empty.getDefaultInstance()).getSuspensionesList();
    Set<Integer> ids = new LinkedHashSet<>();
    for (Suspension suspension : lista) {
      ids.add(suspension.getUsuarioId());
      if (suspension.hasSuspendidaPor()) ids.add(suspension.getSuspendidaPor());
    }
    Map<Integer, Ficha> fichas = consulta.fichas(List.copyOf(ids));

    return lista.stream()
        .filter((suspension) -> fichas.containsKey(suspension.getUsuarioId()))
        .map((suspension) -> new AdminRespuestas.Suspension(
            fichas.get(suspension.getUsuarioId()),
            suspension.getMotivo(),
            suspension.getCreadaEn(),
            suspension.hasSuspendidaPor() ? corta(fichas.get(suspension.getSuspendidaPor())) : null))
        .toList();
  }

  private void resolver(int adminId, int usuarioId, String decision) {
    clientes.moderacion().resolverReportes(ResolverReportesRequest.newBuilder()
        .setAdminId(adminId).setUsuarioId(usuarioId).setDecision(decision).build());
  }

  /** @return null si esa cuenta ya fue borrada */
  private static PersonaCorta corta(Ficha ficha) {
    return ficha == null ? null : ficha.corta();
  }
}
