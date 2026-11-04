package com.roomiesfind.compatibilidad.dto;

import java.util.List;

public record SolicitudExplicacionDto(int usuarioId, double candidatoId, double score,
    List<String> coincidencias, List<String> conflictos) {}
