package com.team10.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Team10Application {

    public static void main(String[] args) {
        SpringApplication.run(Team10Application.class, args);
    }

}
