package com.roomiesfind.perfiles.service;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.perfiles.dto.CandidatoDto;
import com.roomiesfind.perfiles.dto.DatosPerfilDto;
import com.roomiesfind.perfiles.dto.FiltrosCandidatosDto;
import com.roomiesfind.perfiles.dto.FotoDto;
import com.roomiesfind.perfiles.dto.PaginaCandidatosDto;
import com.roomiesfind.perfiles.dto.PerfilDto;
import com.roomiesfind.perfiles.model.Perfil;
import com.roomiesfind.perfiles.repository.FotoRepository;
import com.roomiesfind.perfiles.repository.PerfilRepository;
import com.roomiesfind.perfiles.repository.ZonaRepository;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de perfiles, barrios y fotos. */
@Service
public class PerfilService {

  private static final String PERFIL_NO_ENCONTRADO = "Perfil no encontrado.";
  private static final DateTimeFormatter FECHA_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  private final PerfilRepository perfiles;
  private final ZonaRepository zonas;
  private final FotoRepository fotos;

  public PerfilService(PerfilRepository perfiles, ZonaRepository zonas, FotoRepository fotos) {
    this.perfiles = perfiles;
    this.zonas = zonas;
    this.fotos = fotos;
  }

  @Transactional
  public PerfilDto crear(int usuarioId, DatosPerfilDto datos, FotoDto foto) {
    int zonaId = zonas.obtenerOCrear(datos.zona());
    perfiles.crear(new PerfilRepository.NuevoPerfil(
        usuarioId,
        entero(datos.presupuesto()),
        zonaId,
        datos.horario(),
        datos.limpieza() == null ? 3 : entero(datos.limpieza()),
        datos.toleranciaRuido() == null ? 3 : entero(datos.toleranciaRuido()),
        datos.frecuenciaVisitas() == null ? "ocasional" : datos.frecuenciaVisitas(),
        Boolean.TRUE.equals(datos.tieneMascotas()),
        datos.aceptaMascotas() == null || datos.aceptaMascotas(),
        datos.descripcion() == null || datos.descripcion().isEmpty() ? null : datos.descripcion(),
        foto == null ? null : rutaFoto(usuarioId)));
    if (foto != null) fotos.guardar(usuarioId, foto);
    return obtener(usuarioId);
  }

  public PerfilDto obtener(int usuarioId) {
    return perfiles.buscar(usuarioId).map(PerfilService::aDto)
        .orElseThrow(() -> ErrorNegocio.noEncontrado(PERFIL_NO_ENCONTRADO));
  }

  public List<PerfilDto> obtenerVarios(List<Integer> usuarioIds) {
    return perfiles.buscarVarios(usuarioIds).stream().map(PerfilService::aDto).toList();
  }

  public String zonaActual(int usuarioId) {
    return perfiles.buscar(usuarioId).map(Perfil::zona).orElse(null);
  }

  @Transactional
  public PerfilDto actualizar(int usuarioId, DatosPerfilDto datos) {
    Integer zonaId = datos.zona() == null ? null : zonas.obtenerOCrear(datos.zona());
    perfiles.actualizar(usuarioId, new PerfilRepository.CambiosPerfil(
        enteroOpcional(datos.presupuesto()),
        zonaId,
        datos.horario(),
        enteroOpcional(datos.limpieza()),
        enteroOpcional(datos.toleranciaRuido()),
        datos.frecuenciaVisitas(),
        datos.tieneMascotas(),
        datos.aceptaMascotas(),
        datos.descripcion(),
        datos.fotoUrl()));
    return obtener(usuarioId);
  }

  public PaginaCandidatosDto buscarCandidatos(FiltrosCandidatosDto filtros) {
    PerfilRepository.Conteos conteos = perfiles.contarCandidatos(filtros);
    List<CandidatoDto> candidatos = perfiles.buscarCandidatos(filtros).stream()
        .map((encontrado) -> new CandidatoDto(aDto(encontrado.perfil()), encontrado.puntaje()))
        .toList();
    return new PaginaCandidatosDto(candidatos, conteos.total(), conteos.muyBien(), conteos.buena(),
        conteos.conversar(), conteos.distintos());
  }

  public FotoDto obtenerFoto(int usuarioId) {
    return fotos.buscar(usuarioId)
        .orElseThrow(() -> ErrorNegocio.noEncontrado("Foto de perfil no encontrada."));
  }

  @Transactional
  public String guardarFoto(int usuarioId, FotoDto foto) {
    String ruta = rutaFoto(usuarioId);
    if (!perfiles.actualizarRutaFoto(usuarioId, ruta)) {
      throw ErrorNegocio.noEncontrado(PERFIL_NO_ENCONTRADO);
    }
    fotos.guardar(usuarioId, foto);
    return ruta;
  }

  private static String rutaFoto(int usuarioId) {
    return "/api/perfiles/" + usuarioId + "/foto";
  }

  private static int entero(String valor) {
    return new BigDecimal(valor.trim()).intValueExact();
  }

  private static Integer enteroOpcional(String valor) {
    return valor == null ? null : Integer.valueOf(entero(valor));
  }

  private static PerfilDto aDto(Perfil perfil) {
    return new PerfilDto(
        perfil.usuarioId(), perfil.presupuesto(), perfil.zonaId(), perfil.zona(), perfil.horario(),
        perfil.limpieza(), perfil.toleranciaRuido(), perfil.frecuenciaVisitas(),
        perfil.tieneMascotas(), perfil.aceptaMascotas(), perfil.descripcion(), perfil.fotoUrl(),
        perfil.actualizadoEn().format(FECHA_ISO));
  }
}
