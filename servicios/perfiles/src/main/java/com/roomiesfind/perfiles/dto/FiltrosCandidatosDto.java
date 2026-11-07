package com.roomiesfind.perfiles.dto;

import java.util.List;

/** Los filtros en null o vacíos no se aplican. */
public record FiltrosCandidatosDto(int usuarioId, List<Integer> excluirIds, int pagina, int tamano, String orden,
    List<String> zonas, String horario, Integer presupuestoMaximo, String mascotas, Integer puntajeMinimo) {}
