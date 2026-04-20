package com.workdone.backend.analysis;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class MustHaveGroupConfig {

    public List<MustHaveGroup> groups() {
        return List.of(
                // Język programowania - fundament
                new MustHaveGroup("language", List.of(
                        "java", "java 17", "java 21", "jdk", "jre", "kotlin"
                ), true),

                // Ekosystem Spring
                new MustHaveGroup("framework", List.of(
                        "spring", "spring boot", "springboot", "spring-boot",
                        "spring mvc", "spring security", "spring data"
                ), true),

                // Bazy danych - SQL
                new MustHaveGroup("database", List.of(
                        "sql", "postgres", "postgresql", "mysql", "oracle",
                        "elasticsearch", "hibernate", "jpa"
                ), false)

//                // Architektura i komunikacja
//                new MustHaveGroup("backend_architecture", List.of(
//                        "rest", "restful", "api", "microservices", "mikrousługi",
//                        "graphql", "grpc", "soap", "json", "xml", "swagger", "openapi"
//                ), false),

//                // Chmura i konteneryzacja - standard w 2026
//                new MustHaveGroup("cloud_devops", List.of(
//                        "docker", "kubernetes", "k8s", "aws", "amazon web services",
//                        "azure", "gcp", "google cloud", "terraform", "ci/cd", "jenkins", "github actions"
//                ), false),

//                // Narzędzia budowania i wersjonowania
//                new MustHaveGroup("tools", List.of(
//                        "maven", "gradle", "git", "github", "gitlab", "bitbucket", "jira"
//                ), false),

//                // Testowanie
//                new MustHaveGroup("testing", List.of(
//                        "junit", "mockito", "testcontainers", "assertj", "spock", "selenium", "cucumber"
//                ), false)
        );
    }

    public int minGroupsToPass() {
        return 2;
    }
}