# Imagen de un módulo. Se elige con el argumento MODULO, por ejemplo servicios/usuarios.
FROM maven:3.9-eclipse-temurin-21 AS construccion
ARG MODULO
WORKDIR /codigo
COPY . .
RUN mvn -B -q -pl "$MODULO" -am package -DskipTests \
  && cp "$MODULO"/target/*.jar /aplicacion.jar

FROM eclipse-temurin:21-jre
COPY --from=construccion /aplicacion.jar /aplicacion.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /aplicacion.jar"]
