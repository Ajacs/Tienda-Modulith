package com.example.store;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * No levanta Spring: analiza las clases compiladas. Corre en segundos.
 */
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(StoreApplication.class);

    @Test
    void printsModules() {
        modules.forEach(System.out::println);
    }

    /**
     * Falla si un módulo usa tipos internos de otro, si hay ciclos entre módulos
     * o si alguien depende de un módulo que no está en su allowedDependencies.
     */
    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    /**
     * Genera diagramas PlantUML y un "canvas" por módulo en target/spring-modulith-docs.
     */
    @Test
    void writesDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
