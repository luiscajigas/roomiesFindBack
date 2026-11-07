package com.roomiesfind.perfiles.controller;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.perfiles.dto.FiltrosCandidatosDto;
import java.util.List;

/** Reglas de los filtros y la paginación. */
public final class ReglasBusqueda {

  public static final int TAMANO_POR_DEFECTO = 12;
  private static final int TAMANO_MAXIMO = 50;
  private static final int PAGINA_MAXIMA = 1_000_000;
  private static final int ZONAS_MAXIMAS = 100;
  private static final List<String> ORDENES = List.of("compatibilidad", "presupuesto", "recientes");
  private static final List<String> HORARIOS = List.of("madrugador", "nocturno", "mixto");
  private static final List<String> MASCOTAS = List.of("sin_mascotas", "con_mascotas", "aceptan");

  private ReglasBusqueda() {}

  public static FiltrosCandidatosDto normalizar(FiltrosCandidatosDto f) {
    int pagina = f.pagina() <= 0 ? 1 : f.pagina();
    int tamano = f.tamano() <= 0 ? TAMANO_POR_DEFECTO : f.tamano();
    String orden = f.orden() == null || f.orden().isEmpty() ? ORDENES.get(0) : f.orden();

    if (pagina > PAGINA_MAXIMA) throw invalida("La página no es válida.");
    if (tamano > TAMANO_MAXIMO) throw invalida("Se pueden pedir máximo 50 personas por página.");
    if (!ORDENES.contains(orden)) throw invalida("El orden no es válido.");
    if (f.zonas().size() > ZONAS_MAXIMAS) throw invalida("Elige menos barrios.");
    if (f.horario() != null && !HORARIOS.contains(f.horario())) throw invalida("El horario no es válido.");
    if (f.mascotas() != null && !MASCOTAS.contains(f.mascotas())) throw invalida("El filtro de mascotas no es válido.");
    if (f.presupuestoMaximo() != null && f.presupuestoMaximo() < 0) throw invalida("El presupuesto debe ser un número entero positivo.");
    if (f.puntajeMinimo() != null && (f.puntajeMinimo() < 0 || f.puntajeMinimo() > 100)) throw invalida("La compatibilidad mínima no es válida.");

    return new FiltrosCandidatosDto(f.usuarioId(), f.excluirIds(), pagina, tamano, orden, f.zonas(), f.horario(),
        f.presupuestoMaximo(), f.mascotas(), f.puntajeMinimo());
  }

  private static ErrorNegocio invalida(String mensaje) {
    return ErrorNegocio.solicitudInvalida(mensaje);
  }
}
