
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY Api/ ./Api/
COPY Datalake/ ./Datalake/
COPY Datamart/ ./Datamart/
COPY Orchestrator/ ./Orchestrator/
COPY GUI/ ./GUI/

RUN mvn -q -DskipTests -pl Api -am dependency:go-offline

RUN mvn -q -DskipTests -pl Api -am package

FROM eclipse-temurin:21-jre
ENV TZ=UTC
WORKDIR /opt/app

COPY --from=build /app/Api/target/*.jar app.jar
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=10 CMD \
  sh -c "wget -q -O - http://127.0.0.1:8080/api/health | grep -q 'OK' || exit 1"

ENTRYPOINT ["java","-jar","app.jar"]
