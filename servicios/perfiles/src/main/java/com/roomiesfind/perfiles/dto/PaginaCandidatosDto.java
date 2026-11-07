package com.roomiesfind.perfiles.dto;

import java.util.List;

public record PaginaCandidatosDto(List<CandidatoDto> candidatos, int total, int muyBien, int buena, int conversar, int distintos) {}
