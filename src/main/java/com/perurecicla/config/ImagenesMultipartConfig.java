package com.perurecicla.config;

import jakarta.servlet.MultipartConfigElement;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * PERU RECICLA - PASO 74
 *
 * Permite recibir fotografias de recompensas
 * con un tamaño maximo de 5 MB.
 */
@Configuration
public class ImagenesMultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {

        MultipartConfigFactory factory =
                new MultipartConfigFactory();

        factory.setMaxFileSize(
                DataSize.ofMegabytes(5)
        );

        factory.setMaxRequestSize(
                DataSize.ofMegabytes(6)
        );

        return factory.createMultipartConfig();
    }
}