# Etap 1: Budowanie aplikacji
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etap 2: Środowisko uruchomieniowe (JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Optymalizacja pod QNAP (limit RAM)
ENV JAVA_OPTS="-Xmx512m -Xms256m"

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]