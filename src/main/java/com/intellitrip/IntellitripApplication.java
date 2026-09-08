package com.intellitrip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IntellitripApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntellitripApplication.class, args);
    }
}
