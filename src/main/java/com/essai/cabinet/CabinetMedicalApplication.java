package com.essai.cabinet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;

@SpringBootApplication
public class CabinetMedicalApplication {
    public static void main(String[] args) {
        configureRenderDatabaseUrl();
        SpringApplication.run(CabinetMedicalApplication.class, args);
    }

    private static void configureRenderDatabaseUrl() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank() || System.getenv("SPRING_DATASOURCE_URL") != null) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        String[] userInfo = uri.getUserInfo() == null ? new String[] {"", ""} : uri.getUserInfo().split(":", 2);
        String databaseName = uri.getPath() == null ? "" : uri.getPath();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + databaseName;

        System.setProperty("SPRING_DATASOURCE_URL", jdbcUrl);
        if (userInfo.length > 0 && !userInfo[0].isBlank()) {
            System.setProperty("SPRING_DATASOURCE_USERNAME", userInfo[0]);
        }
        if (userInfo.length > 1 && !userInfo[1].isBlank()) {
            System.setProperty("SPRING_DATASOURCE_PASSWORD", userInfo[1]);
        }
    }
}
