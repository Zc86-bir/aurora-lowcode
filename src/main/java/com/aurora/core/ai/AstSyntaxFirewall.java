package com.aurora.core.ai;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AST Syntax Firewall
 *
 * Uses JavaParser to validate generated Java code at the AST level:
 * 1. Syntax correctness (unmatched brackets, unclosed braces)
 * 2. Illegal annotation detection
 * 3. Dangerous reflection/unsafe code interception
 * 4. Import whitelist enforcement
 *
 * Never trust LLM-generated Java code without AST validation.
 */
public class AstSyntaxFirewall {

    private static final Logger log = LoggerFactory.getLogger(AstSyntaxFirewall.class);

    // Dangerous annotations that could bypass security
    private static final Set<String> DANGEROUS_ANNOTATIONS = Set.of(
        "SuppressWarnings",
        "Rawtypes",
        "unchecked"
    );

    // Dangerous reflection patterns
    private static final Set<String> DANGEROUS_REFLECTION = Set.of(
        "Class.forName",
        "Method.invoke",
        "Field.set",
        "Field.get",
        "Constructor.newInstance",
        "sun.misc.Unsafe",
        "java.lang.invoke.MethodHandles"
    );

    // Import whitelist
    private static final Set<String> ALLOWED_IMPORT_PREFIXES = Set.of(
        "java.",
        "jakarta.",
        "org.springframework.",
        "com.aurora.",
        "io.swagger.",
        "lombok.",
        "org.slf4j.",
        "com.fasterxml.jackson."
    );

    private final JavaParser javaParser;
    private final List<String> customBlockedImports;
    private final boolean blockReflection;
    private final boolean blockUnsafe;

    public AstSyntaxFirewall(boolean blockReflection, boolean blockUnsafe,
                              List<String> customBlockedImports) {
        this.javaParser = new JavaParser();
        this.blockReflection = blockReflection;
        this.blockUnsafe = blockUnsafe;
        this.customBlockedImports = customBlockedImports != null ? customBlockedImports : List.of();
    }

    public AstSyntaxFirewall() {
        this(true, true, List.of());
    }

    /**
     * Validate Java source code via AST analysis.
     */
    public AiSelfCorrectionLoop.AstValidationResult validate(String sourceCode) {
        List<String> errors = new ArrayList<>();

        // Step 1: Parse to AST (catches syntax errors)
        ParseResult<CompilationUnit> parseResult = javaParser.parse(sourceCode);
        if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
            parseResult.getProblems().forEach(p ->
                errors.add("Parse error: " + p.getVerboseMessage())
            );
            return AiSelfCorrectionLoop.AstValidationResult.fail(errors);
        }

        CompilationUnit cu = parseResult.getResult().get();

        // Step 2: Check for parse problems
        parseResult.getProblems().forEach(p ->
            errors.add("AST problem: " + p.getMessage())
        );

        // Step 3: Import whitelist check
        checkImports(cu, errors);

        // Step 4: Dangerous annotation detection
        checkAnnotations(cu, errors);

        // Step 5: Dangerous reflection detection
        if (blockReflection) {
            checkReflection(cu, errors);
        }

        // Step 6: Unsafe code detection
        if (blockUnsafe) {
            checkUnsafe(cu, errors);
        }

        // Step 7: Structural validation
        checkStructure(cu, errors);

        if (!errors.isEmpty()) {
            return AiSelfCorrectionLoop.AstValidationResult.fail(List.copyOf(errors));
        }

        return AiSelfCorrectionLoop.AstValidationResult.ok();
    }

    private void checkImports(CompilationUnit cu, List<String> errors) {
        for (var importDecl : cu.getImports()) {
            String importName = importDecl.getNameAsString();

            // Check blocked imports
            for (String blocked : customBlockedImports) {
                if (importName.startsWith(blocked)) {
                    errors.add("Blocked import: " + importName);
                    break;
                }
            }

            // Check whitelist
            boolean allowed = ALLOWED_IMPORT_PREFIXES.stream()
                .anyMatch(importName::startsWith);
            if (!allowed) {
                errors.add("Import not in whitelist: " + importName);
            }
        }
    }

    private void checkAnnotations(CompilationUnit cu, List<String> errors) {
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                super.visit(n, arg);
                for (var annotation : n.getAnnotations()) {
                    String name = annotation.getNameAsString();
                    if (DANGEROUS_ANNOTATIONS.contains(name)) {
                        log.warn("Potentially dangerous annotation on class: {}", name);
                    }
                }
            }

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                super.visit(n, arg);
                for (var annotation : n.getAnnotations()) {
                    String name = annotation.getNameAsString();
                    if (DANGEROUS_ANNOTATIONS.contains(name)) {
                        log.warn("Potentially dangerous annotation on method: {}", name);
                    }
                }
            }
        }, null);
    }

    private void checkReflection(CompilationUnit cu, List<String> errors) {
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                super.visit(n, arg);
                String fullCall = n.getScope()
                    .map(s -> s.toString() + "." + n.getNameAsString())
                    .orElse(n.getNameAsString());

                for (String dangerous : DANGEROUS_REFLECTION) {
                    if (fullCall.contains(dangerous)) {
                        errors.add("Dangerous reflection call: " + fullCall);
                        break;
                    }
                }
            }
        }, null);
    }

    private void checkUnsafe(CompilationUnit cu, List<String> errors) {
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                super.visit(n, arg);
                if (n.getExtendedTypes().stream()
                    .anyMatch(t -> t.getNameAsString().contains("Unsafe"))) {
                    errors.add("Unsafe usage detected: " + n.getNameAsString());
                }
            }
        }, null);
    }

    private void checkStructure(CompilationUnit cu, List<String> errors) {
        // Validate no empty classes
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                super.visit(n, arg);
                if (n.getMembers().isEmpty() && !n.isInterface()) {
                    errors.add("Empty class: " + n.getNameAsString());
                }
            }

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                super.visit(n, arg);
                if (n.getBody().isEmpty()) {
                    // Empty method body is suspicious in generated code
                    log.warn("Empty method body: {}.{}",
                        n.findAncestor(ClassOrInterfaceDeclaration.class)
                            .map(c -> c.getNameAsString()).orElse("unknown"),
                        n.getNameAsString());
                }
            }
        }, null);
    }
}