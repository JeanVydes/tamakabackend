package com.tamaka.cuentas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.tamaka.cuentas", "com.tamaka.common"})
@ConfigurationPropertiesScan({"com.tamaka.common.security", "com.tamaka.cuentas", "com.tamaka.common.storage"})
public class CuentasApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuentasApplication.class, args);
    }
}
