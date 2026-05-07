package com.aurora.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

class OpenApiAnnotationTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.aurora.core");
    }

    @Test
    @DisplayName("Controller methods must have @Operation annotation")
    void controllerMethodsMustHaveOperationAnnotation() {
        methods()
                .that().areDeclaredInClassesThat()
                .resideInAPackage("..adapter.web..")
                .should().beAnnotatedWith(Operation.class)
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("Controller methods must have @ApiResponse annotation")
    void controllerMethodsMustHaveApiResponseAnnotation() {
        methods()
                .that().areDeclaredInClassesThat()
                .resideInAPackage("..adapter.web..")
                .should().beAnnotatedWith(ApiResponse.class)
                .allowEmptyShould(true)
                .check(classes);
    }
}
