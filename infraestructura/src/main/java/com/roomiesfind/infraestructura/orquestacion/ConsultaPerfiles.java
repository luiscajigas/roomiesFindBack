package com.roomiesfind.infraestructura.orquestacion;

import com.roomiesfind.contratos.moderacion.CuentasRequest;
import com.roomiesfind.contratos.moderacion.UsuarioRequest;
import com.roomiesfind.contratos.perfiles.BuscarCandidatosRequest;
import com.roomiesfind.contratos.perfiles.BuscarCandidatosResponse;
import com.roomiesfind.contratos.perfiles.CandidatoPuntuado;
import com.roomiesfind.contratos.perfiles.ObtenerPerfilRequest;
import com.roomiesfind.contratos.perfiles.ObtenerPerfilesRequest;
import com.roomiesfind.contratos.perfiles.Perfil;
import com.roomiesfind.contratos.usuarios.ObtenerUsuariosRequest;
import com.roomiesfind.contratos.usuarios.Usuario;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * El frontend ve cada perfil junto con el nombre de la persona, pero esos datos
 * viven en dos microservicios. Aquí se unen las dos respuestas.
 */
@Component
public class ConsultaPerfiles {

  private final ClientesGrpc clientes;

  public ConsultaPerfiles(ClientesGrpc clientes) {
    this.clientes = clientes;
  }

  public Map<String, Object> miPerfil(int usuarioId) {
    Perfil perfil = clientes.perfiles().obtenerPerfil(ObtenerPerfilRequest.newBuilder().setUsuarioId(usuarioId).build());
    Usuario usuario = usuarios(List.of(usuarioId)).get(usuarioId);

    Map<String, Object> respuesta = new LinkedHashMap<>();
    respuesta.put("id", usuarioId);
    respuesta.put("nombre", usuario == null ? null : usuario.getNombre());
    respuesta.put("email", usuario == null ? null : usuario.getEmail());
    respuesta.putAll(aMapa(perfil));
    respuesta.put("alerta", conAlerta(List.of(usuarioId)).contains(usuarioId));
    return respuesta;
  }

  /**
   * Una página de personas. Une tres servicios: moderación dice a quién no mostrar,
   * perfiles filtra y ordena, y usuarios pone el nombre.
   */
  public Map<String, Object> buscar(int usuarioId, BuscarCandidatosRequest.Builder peticion) {
    BuscarCandidatosResponse pagina = clientes.perfiles().buscarCandidatos(peticion
        .setUsuarioId(usuarioId)
        .addAllExcluirIds(excluidos(usuarioId))
        .build());
    List<CandidatoPuntuado> encontrados = pagina.getCandidatosList();
    Map<Integer, Integer> puntajes = encontrados.stream()
        .collect(Collectors.toMap((candidato) -> candidato.getPerfil().getUsuarioId(), CandidatoPuntuado::getPuntaje));

    List<Map<String, Object>> candidatos = conNombre(encontrados.stream().map(CandidatoPuntuado::getPerfil).toList());
    candidatos.forEach((candidato) -> candidato.put("puntaje", puntajes.get((Integer) candidato.get("id"))));

    Map<String, Object> resumen = new LinkedHashMap<>();
    resumen.put("muyBien", pagina.getResumen().getMuyBien());
    resumen.put("buena", pagina.getResumen().getBuena());
    resumen.put("conversar", pagina.getResumen().getConversar());
    resumen.put("distintos", pagina.getResumen().getDistintos());

    Map<String, Object> respuesta = new LinkedHashMap<>();
    respuesta.put("candidatos", candidatos);
    respuesta.put("total", pagina.getTotal());
    respuesta.put("resumen", resumen);
    return respuesta;
  }

  /** Quien bloqueó o fue bloqueado no aparece en ninguna parte para la otra persona. */
  public Set<Integer> excluidos(int usuarioId) {
    return Set.copyOf(clientes.moderacion()
        .listarExcluidos(UsuarioRequest.newBuilder().setUsuarioId(usuarioId).build())
        .getIdsList());
  }

  /** Solo las personas pedidas, para no traer a todos los usuarios cuando no hace falta. */
  public List<Map<String, Object>> personas(List<Integer> ids) {
    if (ids.isEmpty()) return List.of();
    return conNombre(clientes.perfiles()
        .obtenerPerfiles(ObtenerPerfilesRequest.newBuilder().addAllUsuarioIds(ids).build())
        .getPerfilesList());
  }

  private List<Map<String, Object>> conNombre(List<Perfil> perfiles) {
    List<Integer> ids = perfiles.stream().map(Perfil::getUsuarioId).toList();
    Map<Integer, Usuario> usuarios = usuarios(ids);
    Set<Integer> conAlerta = conAlerta(ids);

    return perfiles.stream()
        // Un perfil cuyo usuario ya no existe no se muestra.
        .filter((perfil) -> usuarios.containsKey(perfil.getUsuarioId()))
        .map((perfil) -> {
          Map<String, Object> candidato = new LinkedHashMap<>();
          candidato.put("id", perfil.getUsuarioId());
          candidato.put("nombre", usuarios.get(perfil.getUsuarioId()).getNombre());
          candidato.putAll(aMapa(perfil));
          candidato.put("alerta", conAlerta.contains(perfil.getUsuarioId()));
          return candidato;
        })
        .toList();
  }

  /**
   * Nombre, correo, rol y foto de cada persona, para el panel de administración.
   * Incluye a quien no tiene perfil o está suspendido; las cuentas borradas no vienen.
   */
  public Map<Integer, Map<String, Object>> fichas(List<Integer> ids) {
    if (ids.isEmpty()) return Map.of();
    Map<Integer, Perfil> perfiles = clientes.perfiles()
        .obtenerPerfiles(ObtenerPerfilesRequest.newBuilder().addAllUsuarioIds(ids).build())
        .getPerfilesList().stream()
        .collect(Collectors.toMap(Perfil::getUsuarioId, Function.identity()));
    Map<Integer, Map<String, Object>> fichas = new LinkedHashMap<>();
    usuarios(ids).forEach((id, usuario) -> {
      Map<String, Object> ficha = new LinkedHashMap<>();
      ficha.put("id", id);
      ficha.put("nombre", usuario.getNombre());
      ficha.put("email", usuario.getEmail());
      ficha.put("rol", usuario.getRol().isEmpty() ? "usuario" : usuario.getRol());
      Perfil perfil = perfiles.get(id);
      ficha.put("foto_url", perfil != null && perfil.hasFotoUrl() ? perfil.getFotoUrl() : null);
      fichas.put(id, ficha);
    });
    return fichas;
  }

  private Set<Integer> conAlerta(List<Integer> ids) {
    if (ids.isEmpty()) return Set.of();
    return Set.copyOf(clientes.moderacion()
        .cuentasConAlerta(CuentasRequest.newBuilder().addAllIds(ids).build())
        .getIdsList());
  }

  private Map<Integer, Usuario> usuarios(List<Integer> ids) {
    if (ids.isEmpty()) return Map.of();
    return clientes.usuarios().obtenerUsuarios(ObtenerUsuariosRequest.newBuilder().addAllIds(ids).build())
        .getUsuariosList().stream()
        .collect(Collectors.toMap(Usuario::getId, Function.identity()));
  }

  /** Conserva los nombres de campo que ya usa el frontend. */
  public static Map<String, Object> aMapa(Perfil perfil) {
    Map<String, Object> mapa = new LinkedHashMap<>();
    mapa.put("usuario_id", perfil.getUsuarioId());
    mapa.put("presupuesto", perfil.getPresupuesto());
    mapa.put("zona_id", perfil.getZonaId());
    mapa.put("horario", perfil.getHorario());
    mapa.put("limpieza", perfil.getLimpieza());
    mapa.put("tolerancia_ruido", perfil.getToleranciaRuido());
    mapa.put("frecuencia_visitas", perfil.getFrecuenciaVisitas());
    mapa.put("tiene_mascotas", perfil.getTieneMascotas());
    mapa.put("acepta_mascotas", perfil.getAceptaMascotas());
    mapa.put("descripcion", perfil.hasDescripcion() ? perfil.getDescripcion() : null);
    mapa.put("foto_url", perfil.hasFotoUrl() ? perfil.getFotoUrl() : null);
    mapa.put("actualizado_en", perfil.getActualizadoEn());
    mapa.put("zona", perfil.getZona());
    return mapa;
  }
}
