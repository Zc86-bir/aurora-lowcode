package com.aurora.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LayerDependencyTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.aurora.core");
    }

    @Test
    @DisplayName("Domain must not depend on infrastructure")
    void domainShouldNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAnyPackage("..architecture..", "..contract..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Domain must not depend on adapter")
    void domainShouldNotDependOnAdapter() {
        noClasses()
                .that().resideInAnyPackage("..architecture..", "..contract..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Application must not access JPA repositories directly")
    void applicationShouldNotAccessJpaRepositories() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure.database.repository..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Application must not depend on adapter layer")
    void applicationShouldNotDependOnAdapter() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Infrastructure must not depend on adapter")
    void infrastructureShouldNotDependOnAdapter() {
        noClasses()
                .that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Infrastructure must not depend on application")
    void infrastructureShouldNotDependOnApplication() {
        noClasses()
                .that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat()
                .resideInAPackage("..application..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
