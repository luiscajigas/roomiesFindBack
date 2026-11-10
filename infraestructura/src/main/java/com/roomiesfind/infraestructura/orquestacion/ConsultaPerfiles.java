package com.roomiesfind.infraestructura.orquestacion;

import com.google.protobuf.Empty;
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
import com.roomiesfind.infraestructura.contrato.AdminRespuestas.Ficha;
import com.roomiesfind.infraestructura.contrato.PaginaCandidatosRespuesta;
import com.roomiesfind.infraestructura.contrato.PersonaRespuesta;
import com.roomiesfind.infraestructura.contrato.Roles;
import com.roomiesfind.infraestructura.orquestacion.EnParalelo.Par;
import com.roomiesfind.infraestructura.orquestacion.EnParalelo.Trio;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * El frontend ve cada perfil junto con el nombre de la persona y su alerta, pero esos datos
 * viven en tres microservicios. Aquí se unen las respuestas; las llamadas que no dependen
 * entre sí se hacen a la vez.
 */
@Component
public class ConsultaPerfiles {

  private final ClientesGrpc clientes;
  private final EnParalelo enParalelo;

  public ConsultaPerfiles(ClientesGrpc clientes, EnParalelo enParalelo) {
    this.clientes = clientes;
    this.enParalelo = enParalelo;
  }

  public PersonaRespuesta miPerfil(int usuarioId) {
    Trio<Perfil, Map<Integer, Usuario>, Set<Integer>> datos = enParalelo.tres(
        () -> clientes.perfiles().obtenerPerfil(ObtenerPerfilRequest.newBuilder().setUsuarioId(usuarioId).build()),
        () -> usuarios(List.of(usuarioId)),
        () -> conAlerta(List.of(usuarioId)));
    Usuario usuario = datos.segundo().getOrDefault(usuarioId, Usuario.getDefaultInstance());
    return PersonaRespuesta.de(datos.primero(), usuario, datos.tercero().contains(usuarioId))
        .conEmail(usuario.getEmail());
  }

  /**
   * Una página de personas. Moderación y usuarios dicen a quién no mostrar, perfiles filtra
   * y ordena, y luego usuarios y moderación ponen el nombre y la alerta.
   */
  public PaginaCandidatosRespuesta buscar(int usuarioId, BuscarCandidatosRequest.Builder peticion) {
    BuscarCandidatosResponse pagina = clientes.perfiles().buscarCandidatos(peticion
        .setUsuarioId(usuarioId)
        .addAllExcluirIds(excluidos(usuarioId))
        .build());
    Map<Integer, Integer> puntajes = pagina.getCandidatosList().stream()
        .collect(Collectors.toMap((candidato) -> candidato.getPerfil().getUsuarioId(), CandidatoPuntuado::getPuntaje));
    List<PersonaRespuesta> candidatos = conNombre(pagina.getCandidatosList().stream().map(CandidatoPuntuado::getPerfil).toList())
        .stream()
        .map((persona) -> persona.conPuntaje(puntajes.get(persona.id())))
        .toList();
    return new PaginaCandidatosRespuesta(candidatos, pagina.getTotal(), PaginaCandidatosRespuesta.Resumen.de(pagina.getResumen()));
  }

  /**
   * A quién no se le muestra a esta persona: quien bloqueó o fue bloqueado, las cuentas
   * suspendidas y las de administración, que no buscan compañero.
   */
  public Set<Integer> excluidos(int usuarioId) {
    Par<List<Integer>, List<Usuario>> datos = enParalelo.ambos(
        () -> clientes.moderacion().listarExcluidos(UsuarioRequest.newBuilder().setUsuarioId(usuarioId).build()).getIdsList(),
        () -> clientes.usuarios().listarAdministradores(Empty.getDefaultInstance()).getUsuariosList());
    Set<Integer> excluidos = new HashSet<>(datos.primero());
    datos.segundo().stream().map(Usuario::getId).filter((id) -> id != usuarioId).forEach(excluidos::add);
    return Set.copyOf(excluidos);
  }

  /** Solo las personas pedidas, para no traer a todos los usuarios cuando no hace falta. */
  public List<PersonaRespuesta> personas(List<Integer> ids) {
    if (ids.isEmpty()) return List.of();
    return conNombre(perfiles(ids));
  }

  /**
   * Nombre, correo, rol y foto de cada persona, para el panel de administración.
   * Incluye a quien está suspendido; las cuentas borradas no vienen.
   */
  public Map<Integer, Ficha> fichas(List<Integer> ids) {
    if (ids.isEmpty()) return Map.of();
    Par<List<Perfil>, Map<Integer, Usuario>> datos = enParalelo.ambos(() -> perfiles(ids), () -> usuarios(ids));
    Map<Integer, Perfil> perfiles = datos.primero().stream().collect(Collectors.toMap(Perfil::getUsuarioId, Function.identity()));
    Map<Integer, Ficha> fichas = new LinkedHashMap<>();
    datos.segundo().forEach((id, usuario) -> {
      Perfil perfil = perfiles.get(id);
      fichas.put(id, new Ficha(id, usuario.getNombre(), usuario.getEmail(), Roles.normalizar(usuario.getRol()),
          perfil != null && perfil.hasFotoUrl() ? perfil.getFotoUrl() : null));
    });
    return fichas;
  }

  private List<PersonaRespuesta> conNombre(List<Perfil> perfiles) {
    List<Integer> ids = perfiles.stream().map(Perfil::getUsuarioId).toList();
    Par<Map<Integer, Usuario>, Set<Integer>> datos = enParalelo.ambos(() -> usuarios(ids), () -> conAlerta(ids));
    return perfiles.stream()
        // Un perfil cuyo usuario ya no existe no se muestra.
        .filter((perfil) -> datos.primero().containsKey(perfil.getUsuarioId()))
        .map((perfil) -> PersonaRespuesta.de(perfil, datos.primero().get(perfil.getUsuarioId()),
            datos.segundo().contains(perfil.getUsuarioId())))
        .toList();
  }

  private List<Perfil> perfiles(List<Integer> ids) {
    return clientes.perfiles()
        .obtenerPerfiles(ObtenerPerfilesRequest.newBuilder().addAllUsuarioIds(ids).build())
        .getPerfilesList();
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
}
