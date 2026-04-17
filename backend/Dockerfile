# Etap 1: Budowanie
FROM gradle:8.6-jdk21-alpine AS build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle bootJar --no-daemon

# Etap 2: Środowisko (JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV JAVA_OPTS="-Xmx512m -Xms256m"

COPY --from=build /app/build/libs/workdone-backend.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]