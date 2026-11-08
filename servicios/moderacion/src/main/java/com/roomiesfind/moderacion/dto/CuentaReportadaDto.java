package com.roomiesfind.moderacion.dto;

import com.roomiesfind.moderacion.model.Reporte;
import java.util.List;

public record CuentaReportadaDto(int usuarioId, List<Reporte> reportes, boolean alerta, boolean suspendida) {}
