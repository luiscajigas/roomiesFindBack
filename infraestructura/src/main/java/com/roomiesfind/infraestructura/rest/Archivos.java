package com.roomiesfind.infraestructura.rest;

import com.google.protobuf.ByteString;
import com.roomiesfind.contratos.perfiles.Foto;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

/** Prepara un archivo subido para enviarlo por gRPC. Validarlo le corresponde al servicio de perfiles. */
final class Archivos {

  private Archivos() {}

  static Foto aFoto(MultipartFile archivo) {
    try {
      return Foto.newBuilder()
          .setTipoMime(archivo.getContentType() == null ? "" : archivo.getContentType())
          .setContenido(ByteString.copyFrom(archivo.getBytes()))
          .build();
    } catch (IOException error) {
      throw new ErrorRest(HttpStatus.BAD_REQUEST, "No se pudo recibir la foto.");
    }
  }
}
