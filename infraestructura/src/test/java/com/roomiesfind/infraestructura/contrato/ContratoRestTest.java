package com.roomiesfind.infraestructura.contrato;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.roomiesfind.infraestructura.contrato.ChatRespuestas.MensajeRespuesta;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

/**
 * El contrato REST (contratos/src/main/openapi/roomiesfind-api.yaml) se escribe primero.
 * Esta prueba falla si el código se aparta de él: una ruta de más o de menos, o un campo
 * de respuesta con otro nombre. Así el contrato no se queda viejo y los tipos que genera
 * el frontend a partir de él son los que de verdad llegan.
 */
class ContratoRestTest {

  private static final Path CONTRATO = Path.of("..", "contratos", "src", "main", "openapi", "roomiesfind-api.yaml");
  private static final Set<String> METODOS = Set.of("get", "post", "put", "delete", "patch");

  private static Map<String, Object> contrato;

  @BeforeAll
  static void leerContrato() throws IOException {
    try (InputStream archivo = Files.newInputStream(CONTRATO)) {
      contrato = new Yaml().load(archivo);
    }
  }

  @Test
  void cadaRutaDelCodigoEstaEnElContratoYAlReves() throws ClassNotFoundException {
    assertEquals(rutasDelContrato(), rutasDelCodigo(),
        "Las rutas REST no coinciden con roomiesfind-api.yaml. Actualiza el contrato primero y luego el código.");
  }

  @Test
  void cadaRespuestaTieneLosCamposDeSuSchema() {
    Map<String, Class<?>> respuestas = Map.ofEntries(
        Map.entry("Persona", PersonaRespuesta.class),
        Map.entry("PaginaCandidatos", PaginaCandidatosRespuesta.class),
        Map.entry("ResumenNiveles", PaginaCandidatosRespuesta.Resumen.class),
        Map.entry("Mensaje", MensajeRespuesta.class),
        Map.entry("Conversacion", ChatRespuestas.ConversacionRespuesta.class),
        Map.entry("PaginaMensajes", ChatRespuestas.PaginaMensajesRespuesta.class),
        Map.entry("UsuarioSesion", AuthRespuestas.UsuarioSesion.class),
        Map.entry("Sesion", AuthRespuestas.SesionRespuesta.class),
        Map.entry("Ficha", AdminRespuestas.Ficha.class),
        Map.entry("PersonaCorta", AdminRespuestas.PersonaCorta.class),
        Map.entry("ReporteAdmin", AdminRespuestas.Reporte.class),
        Map.entry("CuentaReportada", AdminRespuestas.CuentaReportada.class),
        Map.entry("Suspension", AdminRespuestas.Suspension.class),
        Map.entry("ResumenAdmin", AdminRespuestas.Resumen.class),
        Map.entry("Error", OtrasRespuestas.ErrorRespuesta.class),
        Map.entry("Salud", OtrasRespuestas.Salud.class),
        Map.entry("FotoGuardada", OtrasRespuestas.FotoGuardada.class),
        Map.entry("Registrado", OtrasRespuestas.Registrado.class),
        Map.entry("Traduccion", OtrasRespuestas.Traduccion.class),
        Map.entry("ReporteCreado", OtrasRespuestas.ReporteCreado.class),
        Map.entry("Explicacion", OtrasRespuestas.Explicacion.class));
    respuestas.forEach((schema, clase) ->
        assertEquals(camposDelSchema(schema), camposJson(clase), "El schema " + schema + " no coincide con " + clase.getSimpleName()));
  }

  @Test
  void cadaEventoDelWebSocketTieneLosCamposDeSuSchema() {
    Map<String, Class<?>> eventos = Map.of(
        "EventoListo", EventoNavegador.Listo.class,
        "EventoPong", EventoNavegador.Pong.class,
        "EventoEnviado", EventoNavegador.Enviado.class,
        "EventoError", EventoNavegador.ErrorEnvio.class,
        "EventoMensaje", EventoNavegador.MensajeNuevo.class,
        "EventoLeidos", EventoNavegador.Leidos.class,
        "EventoEscribiendo", EventoNavegador.Escribiendo.class,
        "EventoAlerta", EventoNavegador.Alerta.class,
        "EventoBloqueo", EventoNavegador.Bloqueo.class);
    assertEquals(EventoNavegador.class.getPermittedSubclasses().length, eventos.size(), "Hay un evento sin schema en el contrato");
    eventos.forEach((schema, clase) -> {
      Set<String> campos = camposJson(clase);
      campos.add("tipo");
      assertEquals(camposDelSchema(schema), campos, "El schema " + schema + " no coincide con " + clase.getSimpleName());
    });
  }

  @SuppressWarnings("unchecked")
  private static Set<String> rutasDelContrato() {
    Set<String> rutas = new TreeSet<>();
    ((Map<String, Map<String, Object>>) contrato.get("paths")).forEach((ruta, operaciones) ->
        operaciones.keySet().stream()
            .filter(METODOS::contains)
            .forEach((metodo) -> rutas.add(metodo.toUpperCase() + " " + normalizar(ruta))));
    return rutas;
  }

  private static Set<String> rutasDelCodigo() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
    escaner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    Set<String> rutas = new TreeSet<>();
    for (BeanDefinition definicion : escaner.findCandidateComponents("com.roomiesfind.infraestructura")) {
      Class<?> clase = Class.forName(definicion.getBeanClassName());
      RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(clase, RequestMapping.class);
      String[] prefijos = base == null || base.path().length == 0 ? new String[] {""} : base.path();
      for (Method metodo : clase.getDeclaredMethods()) {
        RequestMapping mapeo = AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class);
        if (mapeo == null) continue;
        String[] sufijos = mapeo.path().length == 0 ? new String[] {""} : mapeo.path();
        for (String prefijo : prefijos) {
          for (String sufijo : sufijos) {
            for (RequestMethod verbo : mapeo.method()) rutas.add(verbo.name() + " " + normalizar(prefijo + sufijo));
          }
        }
      }
    }
    return rutas;
  }

  @SuppressWarnings("unchecked")
  private static Set<String> camposDelSchema(String nombre) {
    Map<String, Object> componentes = (Map<String, Object>) contrato.get("components");
    Map<String, Object> schema = ((Map<String, Map<String, Object>>) componentes.get("schemas")).get(nombre);
    return new TreeSet<>(((Map<String, Object>) schema.get("properties")).keySet());
  }

  private static Set<String> camposJson(Class<?> clase) {
    ObjectMapper json = new ObjectMapper();
    BeanDescription descripcion = json.getSerializationConfig().introspect(json.constructType(clase));
    return descripcion.findProperties().stream().map(BeanPropertyDefinition::getName).collect(Collectors.toCollection(TreeSet::new));
  }

  /** /api/perfiles/{usuarioId} y /api/perfiles/{id} son la misma ruta. */
  private static String normalizar(String ruta) {
    return ruta.replaceAll("\\{[^}]+}", "{}");
  }
}
