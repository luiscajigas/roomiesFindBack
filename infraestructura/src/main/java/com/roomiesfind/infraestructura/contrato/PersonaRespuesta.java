package com.roomiesfind.infraestructura.contrato;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.roomiesfind.contratos.perfiles.Perfil;
import com.roomiesfind.contratos.usuarios.Usuario;

/**
 * Una persona con su perfil, tal como la ve el frontend (schema Persona del contrato OpenAPI).
 * Los nombres en snake_case se conservan por compatibilidad con el backend original.
 *
 * @param email solo viene en el perfil propio
 * @param puntaje solo viene en la búsqueda, donde lo calcula el servidor
 */
public record PersonaRespuesta(
    int id,
    String nombre,
    @JsonInclude(JsonInclude.Include.NON_NULL) String email,
    @JsonProperty("usuario_id") int usuarioId,
    int presupuesto,
    @JsonProperty("zona_id") int zonaId,
    String zona,
    String horario,
    int limpieza,
    @JsonProperty("tolerancia_ruido") int toleranciaRuido,
    @JsonProperty("frecuencia_visitas") String frecuenciaVisitas,
    @JsonProperty("tiene_mascotas") boolean tieneMascotas,
    @JsonProperty("acepta_mascotas") boolean aceptaMascotas,
    String descripcion,
    @JsonProperty("foto_url") String fotoUrl,
    @JsonProperty("actualizado_en") String actualizadoEn,
    boolean alerta,
    @JsonInclude(JsonInclude.Include.NON_NULL) Integer puntaje) {

  public static PersonaRespuesta de(Perfil perfil, Usuario usuario, boolean alerta) {
    return new PersonaRespuesta(
        perfil.getUsuarioId(), usuario.getNombre(), null, perfil.getUsuarioId(), perfil.getPresupuesto(),
        perfil.getZonaId(), perfil.getZona(), perfil.getHorario(), perfil.getLimpieza(), perfil.getToleranciaRuido(),
        perfil.getFrecuenciaVisitas(), perfil.getTieneMascotas(), perfil.getAceptaMascotas(),
        perfil.hasDescripcion() ? perfil.getDescripcion() : null, perfil.hasFotoUrl() ? perfil.getFotoUrl() : null,
        perfil.getActualizadoEn(), alerta, null);
  }

  public PersonaRespuesta conEmail(String correo) {
    return new PersonaRespuesta(id, nombre, correo, usuarioId, presupuesto, zonaId, zona, horario, limpieza,
        toleranciaRuido, frecuenciaVisitas, tieneMascotas, aceptaMascotas, descripcion, fotoUrl, actualizadoEn, alerta, puntaje);
  }

  public PersonaRespuesta conPuntaje(int valor) {
    return new PersonaRespuesta(id, nombre, email, usuarioId, presupuesto, zonaId, zona, horario, limpieza,
        toleranciaRuido, frecuenciaVisitas, tieneMascotas, aceptaMascotas, descripcion, fotoUrl, actualizadoEn, alerta, valor);
  }
}
