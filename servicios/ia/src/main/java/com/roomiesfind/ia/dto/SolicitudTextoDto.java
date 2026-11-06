package com.roomiesfind.ia.dto;

import java.util.List;

public record SolicitudTextoDto(String nombreUsuario, String nombreCandidato, int score,
    List<String> coincidencias, List<String> conflictos, String idioma) {}
