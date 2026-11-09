package com.tamaka.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.tamaka.pagos", "com.tamaka.common"})
@ConfigurationPropertiesScan({"com.tamaka.common.security", "com.tamaka.pagos"})
public class PagosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagosApplication.class, args);
    }
}
