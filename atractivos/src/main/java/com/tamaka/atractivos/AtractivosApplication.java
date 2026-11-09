package com.tamaka.atractivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.tamaka.atractivos", "com.tamaka.common"})
@ConfigurationPropertiesScan("com.tamaka.common.security")
public class AtractivosApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtractivosApplication.class, args);
    }
}
