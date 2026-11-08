package com.roomiesfind.moderacion.model;

/** Fila de la tabla suspensiones. */
public record Suspension(int usuarioId, String motivo, Integer suspendidaPor, String creadaEn) {}
