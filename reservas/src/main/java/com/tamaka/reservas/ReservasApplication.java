package com.tamaka.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.tamaka.reservas", "com.tamaka.common"})
@ConfigurationPropertiesScan({"com.tamaka.common.security", "com.tamaka.reservas"})
@EnableScheduling
public class ReservasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservasApplication.class, args);
    }
}
