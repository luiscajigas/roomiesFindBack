package com.roomiesfind.infraestructura.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomiesfind.contratos.chat.AvisarAlertaRequest;
import com.roomiesfind.contratos.chat.AvisarBloqueoRequest;
import com.roomiesfind.contratos.moderacion.ParejaRequest;
import com.roomiesfind.contratos.moderacion.ReportarRequest;
import com.roomiesfind.contratos.moderacion.UsuarioRequest;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import com.roomiesfind.infraestructura.contrato.OtrasRespuestas.ReporteCreado;
import com.roomiesfind.infraestructura.contrato.PersonaRespuesta;
import com.roomiesfind.infraestructura.orquestacion.ConsultaPerfiles;
import com.roomiesfind.infraestructura.seguridad.AutenticacionInterceptor;
import com.roomiesfind.infraestructura.seguridad.RequiereAuth;
import io.grpc.StatusRuntimeException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bloqueos y reportes. Recibe REST del frontend, guarda en moderación por gRPC
 * y le pide a chat que avise en vivo. Si ese aviso falla no se deshace nada:
 * lo importante ya quedó guardado y se verá al recargar.
 */
@RestController
@RequestMapping("/api/moderacion")
public class ModeracionRest {

  private static final Logger log = LoggerFactory.getLogger(ModeracionRest.class);

  private final ClientesGrpc clientes;
  private final ConsultaPerfiles consulta;

  public ModeracionRest(ClientesGrpc clientes, ConsultaPerfiles consulta) {
    this.clientes = clientes;
    this.consulta = consulta;
  }

  @RequiereAuth
  @GetMapping("/bloqueos")
  List<PersonaRespuesta> bloqueados(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId) {
    return consulta.personas(clientes.moderacion()
        .listarBloqueados(UsuarioRequest.newBuilder().setUsuarioId(usuarioId).build())
        .getIdsList());
  }

  @RequiereAuth
  @PostMapping("/bloqueos/{otroId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void bloquear(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId, @PathVariable String otroId) {
    bloquear(usuarioId, Parametros.id(otroId));
  }

  @RequiereAuth
  @DeleteMapping("/bloqueos/{otroId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void desbloquear(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId, @PathVariable String otroId) {
    int id = Parametros.id(otroId);
    clientes.moderacion().desbloquear(ParejaRequest.newBuilder().setUsuarioId(usuarioId).setOtroId(id).build());
    avisarBloqueo(usuarioId, id);
  }

  @RequiereAuth
  @PostMapping("/reportes")
  @ResponseStatus(HttpStatus.CREATED)
  ReporteCreado reportar(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_USUARIO) int usuarioId,
      @RequestBody(required = false) JsonNode cuerpo) {
    JsonNode datos = Json.objetoOVacio(cuerpo);
    double reportado = Json.tieneValor(datos, "reportadoId") ? Json.comoNumero(datos.get("reportadoId")) : Double.NaN;
    if (!(reportado > 0 && reportado <= Integer.MAX_VALUE && reportado == Math.rint(reportado))) {
      throw new ErrorRest(HttpStatus.BAD_REQUEST, "La solicitud no es válida.");
    }
    int reportadoId = (int) reportado;

    boolean alertaActivada = clientes.moderacion().reportar(ReportarRequest.newBuilder()
        .setReportanteId(usuarioId)
        .setReportadoId(reportadoId)
        .setMotivo(datos.path("motivo").asText(""))
        .setDetalle(datos.path("detalle").asText(""))
        .build()).getAlertaActivada();

    if (alertaActivada) avisarAlerta(reportadoId);
    if (datos.path("bloquear").asBoolean(false)) bloquear(usuarioId, reportadoId);
    return new ReporteCreado(alertaActivada);
  }

  private void bloquear(int usuarioId, int otroId) {
    clientes.moderacion().bloquear(ParejaRequest.newBuilder().setUsuarioId(usuarioId).setOtroId(otroId).build());
    avisarBloqueo(usuarioId, otroId);
  }

  private void avisarAlerta(int usuarioId) {
    try {
      clientes.chat().avisarAlerta(AvisarAlertaRequest.newBuilder().setUsuarioId(usuarioId).build());
    } catch (StatusRuntimeException error) {
      log.warn("No se pudo avisar en vivo de una alerta: {}", error.getStatus().getCode());
    }
  }

  private void avisarBloqueo(int usuarioId, int otroId) {
    try {
      clientes.chat().avisarBloqueo(AvisarBloqueoRequest.newBuilder().setUsuarioId(usuarioId).setOtroId(otroId).build());
    } catch (StatusRuntimeException error) {
      log.warn("No se pudo avisar en vivo de un bloqueo: {}", error.getStatus().getCode());
    }
  }

}
