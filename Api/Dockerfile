
# ---------- Build stage ----------
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos el POM padre (raíz) y los POMs de los módulos para que el reactor exista
COPY pom.xml ./
COPY Api/pom.xml Api/pom.xml
COPY Datalake/pom.xml Datalake/pom.xml
COPY Datamart/pom.xml Datamart/pom.xml
COPY Orchestrator/pom.xml Orchestrator/pom.xml

# Pre-descarga de dependencias SOLO del Api (resuelve el parent y evita descargar en cada build)
RUN mvn -q -e -DskipTests -pl Api -am dependency:go-offline

# Copiamos el código del Api (no es necesario copiar el src de los otros módulos)
COPY Api/src Api/src

# Compilamos únicamente Api (y lo que necesite por dependencia)
RUN mvn -q -e -DskipTests -pl Api -am package

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
ENV TZ=UTC
WORKDIR /opt/app

# Copiamos el jar del Api
COPY --from=build /app/Api/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]