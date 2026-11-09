package com.tamaka.preferencias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.tamaka.preferencias", "com.tamaka.common"})
@ConfigurationPropertiesScan("com.tamaka.common.security")
public class PreferenciasApplication {

    public static void main(String[] args) {
        SpringApplication.run(PreferenciasApplication.class, args);
    }
}
