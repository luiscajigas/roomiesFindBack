package com.roomiesfind.perfiles.dto;

/** Datos que llegan para crear o editar un perfil. Un campo en null no se envió. */
public record DatosPerfilDto(String presupuesto, String zona, String horario, String limpieza,
    String toleranciaRuido, String frecuenciaVisitas, Boolean tieneMascotas, Boolean aceptaMascotas,
    String descripcion, String fotoUrl) {}
