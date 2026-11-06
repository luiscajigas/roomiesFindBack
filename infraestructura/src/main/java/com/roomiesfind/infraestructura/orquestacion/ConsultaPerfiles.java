package com.roomiesfind.infraestructura.orquestacion;

import com.roomiesfind.contratos.perfiles.ListarCandidatosRequest;
import com.roomiesfind.contratos.perfiles.ObtenerPerfilRequest;
import com.roomiesfind.contratos.perfiles.ObtenerPerfilesRequest;
import com.roomiesfind.contratos.perfiles.Perfil;
import com.roomiesfind.contratos.usuarios.ObtenerUsuariosRequest;
import com.roomiesfind.contratos.usuarios.Usuario;
import com.roomiesfind.infraestructura.config.ClientesGrpc;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    return respuesta;
  }

  public List<Map<String, Object>> candidatos(int usuarioId) {
    List<Perfil> perfiles = clientes.perfiles()
        .listarCandidatos(ListarCandidatosRequest.newBuilder().setExcluirUsuarioId(usuarioId).build())
        .getPerfilesList();
    return conNombre(perfiles);
  }

  /** Solo las personas pedidas, para no traer a todos los usuarios cuando no hace falta. */
  public List<Map<String, Object>> personas(List<Integer> ids) {
    if (ids.isEmpty()) return List.of();
    return conNombre(clientes.perfiles()
        .obtenerPerfiles(ObtenerPerfilesRequest.newBuilder().addAllUsuarioIds(ids).build())
        .getPerfilesList());
  }

  private List<Map<String, Object>> conNombre(List<Perfil> perfiles) {
    Map<Integer, Usuario> usuarios = usuarios(perfiles.stream().map(Perfil::getUsuarioId).toList());

    return perfiles.stream()
        // Un perfil cuyo usuario ya no existe no se muestra.
        .filter((perfil) -> usuarios.containsKey(perfil.getUsuarioId()))
        .map((perfil) -> {
          Map<String, Object> candidato = new LinkedHashMap<>();
          candidato.put("id", perfil.getUsuarioId());
          candidato.put("nombre", usuarios.get(perfil.getUsuarioId()).getNombre());
          candidato.putAll(aMapa(perfil));
          return candidato;
        })
        .toList();
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
