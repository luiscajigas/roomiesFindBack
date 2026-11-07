package com.roomiesfind.perfiles.controller;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.perfiles.dto.DatosPerfilDto;
import com.roomiesfind.perfiles.dto.FiltrosCandidatosDto;
import com.roomiesfind.perfiles.dto.FotoDto;
import com.roomiesfind.perfiles.dto.PaginaCandidatosDto;
import com.roomiesfind.perfiles.dto.PerfilDto;
import com.roomiesfind.perfiles.service.PerfilService;
import java.util.List;
import org.springframework.stereotype.Component;

/** Valida la entrada y coordina los casos de uso. No conoce gRPC ni SQL. */
@Component
public class PerfilController {

  private static final String BARRIO_FUERA_DE_LISTA = "Elige un barrio de la lista.";

  private final PerfilService perfiles;
  private final Barrios barrios;

  public PerfilController(PerfilService perfiles, Barrios barrios) {
    this.perfiles = perfiles;
    this.barrios = barrios;
  }

  public void validarRegistro(DatosPerfilDto datos, FotoDto foto) {
    if (!barrios.esValido(datos.zona())) {
      throw ErrorNegocio.solicitudInvalida(BARRIO_FUERA_DE_LISTA);
    }
    ReglasPerfil.validarPreferencias(datos);
    if (foto != null) ReglasPerfil.validarFoto(foto);
  }

  public PerfilDto crear(int usuarioId, DatosPerfilDto datos, FotoDto foto) {
    validarRegistro(datos, foto);
    return perfiles.crear(usuarioId, datos, foto);
  }

  public PerfilDto obtener(int usuarioId) {
    return perfiles.obtener(usuarioId);
  }

  public List<PerfilDto> obtenerVarios(List<Integer> usuarioIds) {
    return perfiles.obtenerVarios(usuarioIds);
  }

  public PerfilDto actualizar(int usuarioId, DatosPerfilDto datos) {
    if (datos.fotoUrl() != null && !datos.fotoUrl().isEmpty() && !ReglasPerfil.esUrlImagenValida(datos.fotoUrl())) {
      throw ErrorNegocio.solicitudInvalida("El enlace de la foto debe ser una URL válida que use HTTP o HTTPS.");
    }
    ReglasPerfil.validarPreferencias(datos);
    // Las cuentas antiguas pueden conservar la zona que escribieron a mano.
    if (datos.zona() != null && !barrios.esValido(datos.zona())
        && !datos.zona().equals(perfiles.zonaActual(usuarioId))) {
      throw ErrorNegocio.solicitudInvalida(BARRIO_FUERA_DE_LISTA);
    }
    return perfiles.actualizar(usuarioId, datos);
  }

  public List<PerfilDto> candidatos(int excluirUsuarioId) {
    return perfiles.candidatos(excluirUsuarioId);
  }

  public PaginaCandidatosDto buscarCandidatos(FiltrosCandidatosDto filtros) {
    return perfiles.buscarCandidatos(ReglasBusqueda.normalizar(filtros));
  }

  public FotoDto obtenerFoto(int usuarioId) {
    return perfiles.obtenerFoto(usuarioId);
  }

  public String guardarFoto(int usuarioId, FotoDto foto) {
    ReglasPerfil.validarFoto(foto);
    return perfiles.guardarFoto(usuarioId, foto);
  }
}
