package com.roomiesfind.perfiles.dto;

public record PerfilDto(int usuarioId, int presupuesto, int zonaId, String zona, String horario, int limpieza,
    int toleranciaRuido, String frecuenciaVisitas, boolean tieneMascotas, boolean aceptaMascotas,
    String descripcion, String fotoUrl, String actualizadoEn) {}
