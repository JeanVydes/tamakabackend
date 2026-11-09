package com.tamaka.indicadores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.tamaka.indicadores", "com.tamaka.common"})
@ConfigurationPropertiesScan("com.tamaka.common.security")
public class IndicadoresApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndicadoresApplication.class, args);
    }
}
