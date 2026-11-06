package com.roomiesfind.chat.dto;

import java.util.List;

/** Mensajes en orden cronológico; hayMas indica si quedan anteriores. */
public record PaginaMensajesDto(List<MensajeDto> mensajes, boolean hayMas) {}
