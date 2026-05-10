package com.mp.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Web API Application - UI Layer Only
 * 
 * This application serves as the UI/presentation layer and does NOT connect to any database.
 * All data operations are handled by core-api via REST API calls.
 * 
 * Architecture:
 * - web-api (UI Layer): Thymeleaf templates, REST controllers, Spring Security
 * - core-api (Business Layer): Database operations, business logic, RBAC
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class WebApiApplication {

    public static String env;
    @Value("${env}")
    public void setEnv(String setEnv) {
        WebApiApplication.env = setEnv;
    }

    public static String site;
    @Value("${site}")
    public void setSite(String site) {
        WebApiApplication.site = site;
    }

    public static String project;
    @Value("${project}")
    public void setProject(String project) {
        WebApiApplication.project = project;
    }

    public static String coreApiServer;
    @Value("${core.api.url}")
    public void setCoreApiServer(String coreApiServer) {
        WebApiApplication.coreApiServer = coreApiServer;
    }

    public static void main(String[] args) {
        SpringApplication.run(WebApiApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return (args) -> {
            try {
                System.out.println("------------------------------- " + site + " - " + project + " -------------------------------");
                System.out.printf("%-25s : %s\n", "env", env);
                System.out.printf("%-25s : %s\n", "site", site);
                System.out.printf("%-25s : %s\n", "project", project);
                System.out.printf("%-25s : %s\n", "coreApiServer", coreApiServer);
                System.out.println("Web API Ready! Connecting to Core API...");
            } catch (Exception ex) {
                System.err.println("Error during startup: " + ex.getMessage());
            } finally {
                System.out.println("------------------------------------------------------------------------------------");
            }
        };
    }
}
