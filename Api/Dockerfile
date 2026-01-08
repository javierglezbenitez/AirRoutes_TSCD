
# ---------- Build stage ----------
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos el POM padre y TODO el código de los módulos que el parent declara
COPY pom.xml ./
COPY Api/ ./Api/
COPY Datalake/ ./Datalake/
COPY Datamart/ ./Datamart/
COPY Orchestrator/ ./Orchestrator/
COPY CLI/ ./CLI/

# Descarga dependencias (aprovecha cache)
RUN mvn -q -DskipTests -pl Api -am dependency:go-offline

# Compila solo lo necesario (Api y lo que requiera)
RUN mvn -q -DskipTests -pl Api -am package

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /opt/app
COPY --from=build /app/Api/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
