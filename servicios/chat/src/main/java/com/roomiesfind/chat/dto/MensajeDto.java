package com.roomiesfind.chat.dto;

public record MensajeDto(long id, int remitenteId, int destinatarioId, String contenido, String creadoEn, String leidoEn) {}
