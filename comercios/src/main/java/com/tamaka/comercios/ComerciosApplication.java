package com.tamaka.comercios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.tamaka.comercios", "com.tamaka.common"})
@ConfigurationPropertiesScan({"com.tamaka.common.security", "com.tamaka.common.storage"})
public class ComerciosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComerciosApplication.class, args);
    }
}
