package com.team10.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;


@EnableRetry
@SpringBootApplication
@EnableJpaAuditing
public class Team10Application {

    public static void main(String[] args) {
        SpringApplication.run(Team10Application.class, args);
    }

}
