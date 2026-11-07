package com.roomiesfind.perfiles.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.roomiesfind.comun.ErrorNegocio;
import com.roomiesfind.perfiles.dto.FiltrosCandidatosDto;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReglasBusquedaTest {

  private static FiltrosCandidatosDto filtros(int pagina, int tamano, String orden, List<String> zonas, String horario,
      Integer presupuesto, String mascotas, Integer puntaje) {
    return new FiltrosCandidatosDto(1, List.of(), pagina, tamano, orden, zonas, horario, presupuesto, mascotas, puntaje);
  }

  private static void rechaza(FiltrosCandidatosDto datos) {
    assertThrows(ErrorNegocio.class, () -> ReglasBusqueda.normalizar(datos));
  }

  @Test
  void usaValoresPorDefectoSiNoLlegan() {
    FiltrosCandidatosDto resultado = ReglasBusqueda.normalizar(filtros(0, 0, "", List.of(), null, null, null, null));
    assertEquals(1, resultado.pagina());
    assertEquals(ReglasBusqueda.TAMANO_POR_DEFECTO, resultado.tamano());
    assertEquals("compatibilidad", resultado.orden());
  }

  @Test
  void aceptaFiltrosValidos() {
    FiltrosCandidatosDto resultado = ReglasBusqueda.normalizar(
        filtros(2, 50, "presupuesto", List.of("Palermo"), "nocturno", 0, "aceptan", 100));
    assertEquals(2, resultado.pagina());
    assertEquals(50, resultado.tamano());
  }

  @Test
  void rechazaLoQueNoEstaEnLasListas() {
    rechaza(filtros(1, 12, "nombre; DROP TABLE perfiles", List.of(), null, null, null, null));
    rechaza(filtros(1, 12, "", List.of(), "de dia", null, null, null));
    rechaza(filtros(1, 12, "", List.of(), null, null, "gatos", null));
  }

  @Test
  void rechazaNumerosFueraDeRango() {
    rechaza(filtros(1, 51, "", List.of(), null, null, null, null));
    rechaza(filtros(1_000_001, 12, "", List.of(), null, null, null, null));
    rechaza(filtros(1, 12, "", List.of(), null, -1, null, null));
    rechaza(filtros(1, 12, "", List.of(), null, null, null, 101));
    rechaza(filtros(1, 12, "", Collections.nCopies(101, "Palermo"), null, null, null, null));
  }
}
