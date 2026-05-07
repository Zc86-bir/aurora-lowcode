package com.aurora.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class NamingConventionTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.aurora.core");
    }

    @Test
    @DisplayName("Entity classes must be in infrastructure.database.entity")
    void entitiesShouldBeInCorrectPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Entity")
                .should().resideInAPackage("..infrastructure.database.entity..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Controller classes must be in adapter.web")
    void controllersShouldBeInCorrectPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..adapter.web..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Filter classes should be in adapter layer")
    void filtersShouldBeInAdapterLayer() {
        classes()
                .that().haveSimpleNameEndingWith("Filter")
                .and().areNotInterfaces()
                .should().resideInAnyPackage("..adapter..", "..infrastructure.config..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Config classes should be in infrastructure or adapter")
    void configClassesShouldBeInCorrectPackage() {
        classes()
                .that().haveSimpleNameEndingWith("Config")
                .and().areNotInterfaces()
                .should().resideInAnyPackage("..infrastructure.config..", "..adapter..", "..infrastructure.database..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
