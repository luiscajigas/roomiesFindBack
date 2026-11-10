package com.roomiesfind.infraestructura.contrato;

import com.roomiesfind.contratos.perfiles.ResumenNiveles;
import java.util.List;

/** Una página de la búsqueda de compañeros, con los totales de todas las páginas. */
public record PaginaCandidatosRespuesta(List<PersonaRespuesta> candidatos, int total, Resumen resumen) {

  /** Cuántas personas hay en cada nivel de compatibilidad. */
  public record Resumen(int muyBien, int buena, int conversar, int distintos) {

    public static Resumen de(ResumenNiveles niveles) {
      return new Resumen(niveles.getMuyBien(), niveles.getBuena(), niveles.getConversar(), niveles.getDistintos());
    }
  }
}
