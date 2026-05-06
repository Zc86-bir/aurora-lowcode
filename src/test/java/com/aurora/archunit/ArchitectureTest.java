package com.aurora.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * ArchUnit test enforcing hexagonal architecture + DDD boundaries.
 *
 * <p>Note: ArchUnit 1.4.0 may have limited Java 25 bytecode support.
 * Tests use allowEmptyShould(true) as a safety net.
 * Upgrade to ArchUnit 1.5.0+ when available for full Java 25 support.
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .importPackages("com.aurora.core");
    }

    @Test
    @DisplayName("Hexagonal architecture: layer dependency rules")
    void hexagonalArchitectureLayers() {
        layeredArchitecture()
                .consideringOnlyDependenciesInAnyPackage("com.aurora.core..")
                .layer("Adapter").definedBy("..adapter..")
                .layer("Application").definedBy("..application..")
                .layer("Domain").definedBy("..architecture..", "..contract..")
                .layer("Infrastructure").definedBy("..infrastructure..")
                .layer("AI").definedBy("..ai..")
                .layer("Runtime").definedBy("..runtime..")
                .layer("Generator").definedBy("..generator..")

                .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
                .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Application", "Adapter")

                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Domain layer must not depend on infrastructure or adapter")
    void domainShouldNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAnyPackage("..architecture..", "..contract..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..adapter..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Application layer must not directly use JPA repositories")
    void applicationShouldNotUseJpaRepository() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure.database.repository..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Contract interfaces should not depend on infrastructure implementations")
    void contractShouldNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("..contract..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..adapter..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
