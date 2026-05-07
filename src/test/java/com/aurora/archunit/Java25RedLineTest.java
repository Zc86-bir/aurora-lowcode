package com.aurora.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class Java25RedLineTest {

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.aurora.core");
    }

    @Test
    @DisplayName("No ThreadLocal usage in production code (except tenancy filter compatibility)")
    void noThreadLocalUsage() {
        noClasses()
                .that().resideOutsideOfPackage("..test..")
                .and().resideOutsideOfPackage("..infrastructure.tenancy..")
                .should().dependOnClassesThat()
                .areAssignableTo(ThreadLocal.class)
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    @DisplayName("No CompletableFuture usage in production code")
    void noCompletableFutureUsage() {
        noClasses()
                .that().resideOutsideOfPackage("..test..")
                .should().dependOnClassesThat()
                .areAssignableTo(java.util.concurrent.CompletableFuture.class)
                .allowEmptyShould(true)
                .check(productionClasses);
    }
}
