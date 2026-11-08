package com.roomiesfind.moderacion.model;

/** Fila de la tabla reportes. */
public record Reporte(long id, int reportanteId, int reportadoId, String motivo, String detalle, String estado, String creadoEn) {}
