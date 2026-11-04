package com.roomiesfind.perfiles.model;

import java.time.LocalDateTime;

/** Fila de la tabla perfiles con el nombre de su zona. */
public record Perfil(int usuarioId, int presupuesto, int zonaId, String zona, String horario, int limpieza,
    int toleranciaRuido, String frecuenciaVisitas, boolean tieneMascotas, boolean aceptaMascotas,
    String descripcion, String fotoUrl, LocalDateTime actualizadoEn) {}
