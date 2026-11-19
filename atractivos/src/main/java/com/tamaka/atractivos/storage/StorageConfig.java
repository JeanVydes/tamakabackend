package com.tamaka.atractivos.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tamaka.common.storage.ImageStorage;
import com.tamaka.common.storage.StorageProperties;

/** Cablea el bucket de imagenes (R2). Sin TAMAKA_STORAGE_* el servicio responde 503 al subir fotos. */
@Configuration
public class StorageConfig {

    @Bean
    public ImageStorage imageStorage(StorageProperties properties) {
        return new ImageStorage(properties);
    }
}
