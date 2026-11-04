package com.roomiesfind.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ConexionBaseDatosTest {

  @Test
  void convierteLaUrlDeNeonYDescartaParametrosQueJdbcNoEntiende() {
    ConexionBaseDatos conexion = ConexionBaseDatos.desdeUrl(
        "postgresql://luis:cl%40ve@ep-x.us-east-1.aws.neon.tech/roomies?sslmode=require&channel_binding=require", true);

    assertEquals("jdbc:postgresql://ep-x.us-east-1.aws.neon.tech/roomies?prepareThreshold=0&sslmode=require", conexion.jdbcUrl());
    assertEquals("luis", conexion.usuario());
    assertEquals("cl@ve", conexion.clave());
  }

  @Test
  void conservaElPuertoYNoExigeSslEnDesarrollo() {
    ConexionBaseDatos conexion = ConexionBaseDatos.desdeUrl("postgres://u:p@localhost:5432/roomies", false);
    assertEquals("jdbc:postgresql://localhost:5432/roomies?prepareThreshold=0", conexion.jdbcUrl());
  }

  @Test
  void exigeSslEnProduccionAunqueLaUrlNoLoDiga() {
    ConexionBaseDatos conexion = ConexionBaseDatos.desdeUrl("postgres://u:p@db.render.com/roomies", true);
    assertEquals("jdbc:postgresql://db.render.com/roomies?prepareThreshold=0&sslmode=require", conexion.jdbcUrl());
  }

  @Test
  void noMuestraLaClaveEnLosLogs() {
    assertFalse(ConexionBaseDatos.desdeUrl("postgres://u:secreta@h/db", false).toString().contains("secreta"));
  }
}
