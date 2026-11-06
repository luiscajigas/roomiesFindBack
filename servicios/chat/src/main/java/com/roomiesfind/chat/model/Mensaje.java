package com.roomiesfind.chat.model;

import java.time.Instant;

/** Fila de la tabla mensajes. */
public record Mensaje(long id, long conversacionId, int remitenteId, String contenido, Instant creadoEn, Instant leidoEn) {}
