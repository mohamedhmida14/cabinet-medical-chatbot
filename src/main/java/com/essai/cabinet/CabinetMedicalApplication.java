package com.essai.cabinet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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
        String[] userInfo = uri.getRawUserInfo() == null ? new String[] {"", ""} : uri.getRawUserInfo().split(":", 2);
        String databaseName = uri.getPath() == null ? "" : uri.getPath();
        String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + port + databaseName;

        System.setProperty("SPRING_DATASOURCE_URL", jdbcUrl);
        if (userInfo.length > 0 && !userInfo[0].isBlank()) {
            System.setProperty("SPRING_DATASOURCE_USERNAME", decodeUrlPart(userInfo[0]));
        }
        if (userInfo.length > 1 && !userInfo[1].isBlank()) {
            System.setProperty("SPRING_DATASOURCE_PASSWORD", decodeUrlPart(userInfo[1]));
        }
    }

    private static String decodeUrlPart(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
