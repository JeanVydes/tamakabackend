package com.tamaka.recomendaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.tamaka.recomendaciones", "com.tamaka.common"})
@ConfigurationPropertiesScan("com.tamaka.common.security")
public class RecomendacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecomendacionesApplication.class, args);
    }
}
