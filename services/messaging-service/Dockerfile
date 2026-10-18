# syntax=docker/dockerfile:1.7
# messaging-service, built from this folder alone:
#   docker build -t moodle/messaging-service .

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
# Dependencies first, so editing sources does not re-download them.
COPY pom.xml spotbugs-exclude.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -q dependency:go-offline || true
COPY src/ src/
# Tests and static checks run in CI, not in the image build.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests -Dspotless.check.skip=true -Dspotbugs.skip=true package \
 && cp target/*-SNAPSHOT.jar /app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --home-dir /app app \
 && mkdir -p /app \
 && chown -R app /app
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
USER app
EXPOSE 8082
# Size the heap from the container's memory limit; die rather than limp on OOM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
