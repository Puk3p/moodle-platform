# syntax=docker/dockerfile:1.7
# One image recipe for every Spring Boot application in the repo; pick it with
#   --build-arg MODULE=backend | services/messaging-service | edge/gateway
# Build context: the repository root.

FROM maven:3.9.11-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
# Dependencies first, so editing sources does not re-download them.
COPY ${MODULE}/pom.xml ${MODULE}/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -f ${MODULE}/pom.xml dependency:go-offline || true
COPY ${MODULE}/ ${MODULE}/
# Tests and static checks run in CI, not in the image build.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -f ${MODULE}/pom.xml -DskipTests -Dspotless.check.skip=true -Dspotbugs.skip=true package \
 && cp ${MODULE}/target/*-SNAPSHOT.jar /app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --home-dir /app app \
 && mkdir -p /app/uploads /app/profile-pictures \
 && chown -R app /app
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
USER app
# Size the heap from the container's memory limit; die rather than limp on OOM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
