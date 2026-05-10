package com.mp.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class CoreApiApplication {

    public static String env;
    @Value("${env}")
    public void setEnv(String setEnv) {
        CoreApiApplication.env = setEnv;
    }

    public static String site;
    @Value("${site}")
    public void setSite(String site) {
        CoreApiApplication.site = site;
    }

    public static String project;
    @Value("${project}")
    public void setProject(String project) {
        CoreApiApplication.project = project;
    }

    public static void main(String[] args) {
        SpringApplication.run(CoreApiApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void displayBanner() {
        try {
            Thread.sleep(500);
            System.out.println("------------------------------- " + site + " - " + project + " -------------------------------");
            System.out.printf("%-25s : %s\n", "env", env);
            System.out.printf("%-25s : %s\n", "site", site);
            System.out.printf("%-25s : %s\n", "project", project);
            System.out.println("RBAC System Ready!");
            System.out.println("------------------------------------------------------------------------------------");
        } catch (Exception ex) {
            System.err.println("Error during startup: " + ex.getMessage());
        }
    }
}
