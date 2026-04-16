package com.example.garbagereporting;

import com.example.garbagereporting.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class GarbageReportingApplication {

    public static void main(String[] args) {
        SpringApplication.run(GarbageReportingApplication.class, args);
    }
}
