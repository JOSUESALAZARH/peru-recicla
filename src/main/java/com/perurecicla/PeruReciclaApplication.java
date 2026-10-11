
package com.perurecicla;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PERU RECICLA
 * Ecosistema Digital de Incentivos
 * para el Reciclaje.
 * Proyecto desarrollado con:
 * Java 21
 * Spring Boot 3.4.2
 * Maven
 * MySQL
 * JPA / Hibernate
 * Thymeleaf
 * Spring Security
 * JasperReports
 */

@SpringBootApplication
public class PeruReciclaApplication {

    public static void main(String[] args) {

        SpringApplication.run(
            PeruReciclaApplication.class,
            args
        );

    }

}
