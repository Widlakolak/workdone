plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.workdone"
version = "0.0.1-SNAPSHOT"
description = "AI Recruitment Assistant Backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    // Repozytorium dla wersji Milestone Spring AI
    maven { url = uri("https://repo.spring.io/milestone") }
}

// Zarządzanie wersjami Spring AI
dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0-M6")
    }
}

dependencies {
    // === AI & WEKTORY ===
    implementation("org.springframework.ai:spring-ai-google-ai-gemini-spring-boot-starter")
    implementation("org.springframework.ai:spring-ai-pgvector-store-spring-boot-starter")

    // === BAZA DANYCH ===
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // === WEB & CORE ===
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // === SYSTEMY ROZPROSZONE ===
    implementation("org.springframework.kafka:spring-kafka")

    // === NARZĘDZIA (OpenAPI / Swagger) ===
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

    // === LOMBOK & MAPSTRUCT ===
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // === TESTY ===
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("workdone-backend.jar")
}

springBoot {
    buildInfo()
}