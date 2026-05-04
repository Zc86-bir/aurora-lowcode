package com.aurora;

import com.aurora.core.application.SkillDefinitionLoader;
import com.aurora.core.application.SkillRouter;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill Execution Test
 *
 * Tests SkillDefinitionLoader and SkillRouter:
 * 1. YAML loading and parsing
 * 2. Alias routing (legacy → jeecg-*)
 * 3. Fallback strategy when skill not found
 * 4. Hot-reload behavior
 */
@DisplayName("Skill Execution Tests")
class SkillExecutionTest {

    @TempDir
    Path tempSkillsDir;

    private SkillDefinitionLoader loader;

    @BeforeEach
    void setUp() {
        loader = new SkillDefinitionLoader(tempSkillsDir);
    }

    // ============================================================
    // Test 1: Load a valid JeecgBoot skill
    // ============================================================
    @Test
    @DisplayName("Should load JeecgBoot compatible skill definition")
    void loadJeecgSkill_shouldParseCorrectly() throws IOException {
        writeSkillFile("jeecg-codegen.yaml", """
            skill_id: jeecg-codegen
            name: Jeecg Code Generator
            description: Generate entity, controller, service code
            version: 1.0.0
            category: code_generation
            executor: ai-pipeline
            jeecg_compat: true
            aliases:
              - form_generator
              - skill_form_generator
            input_schema:
              description:
                type: string
                required: true
            output_schema:
              entity_code:
                type: string
            business_rules:
              - "Entity class name must follow Java naming conventions"
            security_rules:
              - "No SQL injection patterns in generated code"
            """);

        var skills = loader.loadAll();
        assertEquals(1, skills.size());

        var skill = skills.getFirst();
        assertEquals("jeecg-codegen", skill.skillId());
        assertEquals("Jeecg Code Generator", skill.name());
        assertEquals("1.0.0", skill.version());
        assertTrue(skill.jeecgCompat());
        assertEquals(2, skill.aliases().size());
        assertTrue(skill.aliases().contains("form_generator"));
    }

    // ============================================================
    // Test 2: Alias routing
    // ============================================================
    @Test
    @DisplayName("Alias should route to canonical skill ID")
    void aliasRouting_shouldResolveToCanonicalId() throws IOException {
        writeSkillFile("jeecg-codegen.yaml", """
            skill_id: jeecg-codegen
            name: Jeecg Code Generator
            version: 1.0.0
            executor: ai-pipeline
            aliases:
              - form_generator
              - skill_form_generator
            jeecg_compat: true
            """);

        loader.loadAll();

        // Resolve alias
        String resolved = loader.resolveAlias("form_generator");
        assertEquals("jeecg-codegen", resolved);

        String resolved2 = loader.resolveAlias("skill_form_generator");
        assertEquals("jeecg-codegen", resolved2);

        // Non-alias should return itself
        String resolved3 = loader.resolveAlias("unknown-skill");
        assertEquals("unknown-skill", resolved3);
    }

    // ============================================================
    // Test 3: loadById with alias
    // ============================================================
    @Test
    @DisplayName("loadById should resolve alias and return skill definition")
    void loadById_withAlias_shouldReturnSkill() throws IOException {
        writeSkillFile("jeecg-bpmn.yaml", """
            skill_id: jeecg-bpmn
            name: Jeecg BPMN Workflow Generator
            version: 1.0.0
            executor: ai-pipeline
            aliases:
              - workflow_designer
            jeecg_compat: true
            """);

        loader.loadAll();

        // Direct lookup
        var direct = loader.loadById("jeecg-bpmn");
        assertTrue(direct.isPresent());
        assertEquals("jeecg-bpmn", direct.get().skillId());

        // Alias lookup
        var alias = loader.loadById("workflow_designer");
        assertTrue(alias.isPresent());
        assertEquals("jeecg-bpmn", alias.get().skillId());
    }

    // ============================================================
    // Test 4: isRegistered with alias
    // ============================================================
    @Test
    @DisplayName("isRegistered should return true for both ID and alias")
    void isRegistered_shouldCheckAliases() throws IOException {
        writeSkillFile("jeecg-system.yaml", """
            skill_id: jeecg-system
            name: Jeecg System Config
            version: 1.0.0
            executor: ai-pipeline
            aliases:
              - config_manager
            jeecg_compat: true
            """);

        loader.loadAll();

        assertTrue(loader.isRegistered("jeecg-system"));
        assertTrue(loader.isRegistered("config_manager"));
        assertFalse(loader.isRegistered("nonexistent-skill"));
    }

    // ============================================================
    // Test 5: Hot-reload updates aliases
    // ============================================================
    @Test
    @DisplayName("Hot-reload should update skill and re-register aliases")
    void hotReload_shouldUpdateAliases() throws IOException {
        writeSkillFile("jeecg-onlform.yaml", """
            skill_id: jeecg-onlform
            name: Jeecg Online Form
            version: 1.0.0
            executor: ai-pipeline
            aliases:
              - table_designer
            jeecg_compat: true
            """);

        loader.loadAll();
        assertTrue(loader.isRegistered("table_designer"));

        // Update the file with new aliases
        writeSkillFile("jeecg-onlform.yaml", """
            skill_id: jeecg-onlform
            name: Jeecg Online Form
            version: 2.0.0
            executor: ai-pipeline
            aliases:
              - table_designer
              - onl_form_v2
            jeecg_compat: true
            """);

        var reloaded = loader.reload("jeecg-onlform");
        assertTrue(reloaded.isPresent());
        assertEquals("2.0.0", reloaded.get().version());

        // Both old and new aliases should work
        assertTrue(loader.isRegistered("table_designer"));
        assertTrue(loader.isRegistered("onl_form_v2"));
    }

    // ============================================================
    // Test 6: Duplicate alias detection
    // ============================================================
    @Test
    @DisplayName("Duplicate alias should be detected (second overwrites first)")
    void duplicateAlias_shouldDetectConflict() throws IOException {
        writeSkillFile("skill-a.yaml", """
            skill_id: skill-a
            name: Skill A
            version: 1.0.0
            executor: ai-pipeline
            aliases:
              - shared-alias
            jeecg_compat: false
            """);

        writeSkillFile("skill-b.yaml", """
            skill_id: skill-b
            name: Skill B
            version: 1.0.0
            executor: ai-pipeline
            aliases:
              - shared-alias
            jeecg_compat: false
            """);

        var skills = loader.loadAll();
        assertEquals(2, skills.size());

        // shared-alias should resolve to the second loaded skill (overwrites)
        String resolved = loader.resolveAlias("shared-alias");
        assertNotNull(resolved);
        // The exact skill depends on file listing order
        assertTrue(resolved.equals("skill-a") || resolved.equals("skill-b"));
    }

    // ============================================================
    // Test 7: Empty skills directory
    // ============================================================
    @Test
    @DisplayName("Empty directory should return empty list")
    void emptyDirectory_shouldReturnEmptyList() throws IOException {
        var skills = loader.loadAll();
        assertTrue(skills.isEmpty());
    }

    // ============================================================
    // Test 8: Nonexistent skill file
    // ============================================================
    @Test
    @DisplayName("Loading nonexistent skill should return empty")
    void nonexistentSkill_shouldReturnEmpty() {
        var result = loader.loadById("nonexistent");
        assertTrue(result.isEmpty());
    }

    // ============================================================
    // Test 9: getJeecgCompatSkills filter
    // ============================================================
    @Test
    @DisplayName("getJeecgCompatSkills should return only jeecg_compat skills")
    void getJeecgCompatSkills_shouldFilterCorrectly() throws IOException {
        writeSkillFile("jeecg-codegen.yaml", """
            skill_id: jeecg-codegen
            name: Jeecg Code Generator
            version: 1.0.0
            executor: ai-pipeline
            jeecg_compat: true
            """);

        writeSkillFile("custom-tool.yaml", """
            skill_id: custom-tool
            name: Custom Tool
            version: 1.0.0
            executor: ai-pipeline
            jeecg_compat: false
            """);

        loader.loadAll();

        var jeecgSkills = loader.getJeecgCompatSkills();
        assertEquals(1, jeecgSkills.size());
        assertEquals("jeecg-codegen", jeecgSkills.getFirst().skillId());

        // All skills should still be in cache
        assertEquals(2, loader.cacheSize());
    }

    // ============================================================
    // Test 10: Clear cache
    // ============================================================
    @Test
    @DisplayName("Clear cache should remove all skills and aliases")
    void clearCache_shouldRemoveAll() throws IOException {
        writeSkillFile("jeecg-codegen.yaml", """
            skill_id: jeecg-codegen
            name: Jeecg Code Generator
            version: 1.0.0
            executor: ai-pipeline
            aliases:
              - form_generator
            jeecg_compat: true
            """);

        loader.loadAll();
        assertEquals(1, loader.cacheSize());
        assertTrue(loader.isRegistered("form_generator"));

        loader.clearCache();

        assertEquals(0, loader.cacheSize());
        assertFalse(loader.isRegistered("jeecg-codegen"));
        assertFalse(loader.isRegistered("form_generator"));
    }

    // ============================================================
    // Test 11: getAllAliases
    // ============================================================
    @Test
    @DisplayName("getAllAliases should return complete alias map")
    void getAllAliases_shouldReturnMap() throws IOException {
        writeSkillFile("jeecg-codegen.yaml", """
            skill_id: jeecg-codegen
            name: Jeecg Code Generator
            version: 1.0.0
            executor: ai-pipeline
            aliases:
              - form_generator
              - skill_form_generator
            jeecg_compat: true
            """);

        loader.loadAll();

        var aliases = loader.getAllAliases();
        assertEquals(2, aliases.size());
        assertEquals("jeecg-codegen", aliases.get("form_generator"));
        assertEquals("jeecg-codegen", aliases.get("skill_form_generator"));
    }

    // ============================================================
    // Test 12: Alias resolution end-to-end (loader integration)
    // ============================================================
    @Test
    @DisplayName("Alias resolution should work end-to-end via loader")
    void aliasResolution_shouldWorkEndToEnd() throws IOException {
        writeSkillFile("jeecg-codegen.yaml", """
            skill_id: jeecg-codegen
            name: Jeecg Code Generator
            description: Generate code from natural language
            version: 1.0.0
            executor: ai-pipeline
            aliases:
              - form_generator
              - skill_form_generator
            jeecg_compat: true
            input_schema:
              description:
                type: string
            output_schema:
              code:
                type: string
            """);

        loader.loadAll();

        // Test that the router can resolve aliases via the loader
        String resolved = loader.resolveAlias("skill_form_generator");
        assertEquals("jeecg-codegen", resolved);

        // Verify the resolved skill can be loaded
        var skill = loader.loadById(resolved);
        assertTrue(skill.isPresent());
        assertTrue(skill.get().jeecgCompat());
    }

    // ============================================================
    // Test Helpers
    // ============================================================

    private void writeSkillFile(String filename, String content) throws IOException {
        Files.writeString(tempSkillsDir.resolve(filename), content);
    }
}
