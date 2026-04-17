# Etap 1: Budowanie
FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon
COPY src ./src

RUN ./gradlew bootJar -x test --no-daemon

# Etap 2: Środowisko (JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV JAVA_OPTS="-Xmx512m -Xms256m"

COPY --from=build /app/build/libs/workdone-backend.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]