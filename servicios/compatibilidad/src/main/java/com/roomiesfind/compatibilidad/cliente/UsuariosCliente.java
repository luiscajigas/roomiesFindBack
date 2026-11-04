package com.roomiesfind.compatibilidad.cliente;

import com.roomiesfind.contratos.usuarios.ObtenerUsuariosRequest;
import com.roomiesfind.contratos.usuarios.Usuario;
import com.roomiesfind.contratos.usuarios.UsuariosGrpc;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Cliente gRPC del microservicio de usuarios. */
@Component
public class UsuariosCliente {

  private final UsuariosGrpc.UsuariosBlockingStub usuarios;

  public UsuariosCliente(UsuariosGrpc.UsuariosBlockingStub usuarios) {
    this.usuarios = usuarios;
  }

  public Map<Integer, String> nombres(List<Integer> ids) {
    return usuarios.withDeadlineAfter(15, TimeUnit.SECONDS)
        .obtenerUsuarios(ObtenerUsuariosRequest.newBuilder().addAllIds(ids).build())
        .getUsuariosList().stream()
        .collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));
  }
}
