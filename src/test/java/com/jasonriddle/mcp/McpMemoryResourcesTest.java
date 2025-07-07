package com.jasonriddle.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonriddle.mcp.memory.Entity;
import com.jasonriddle.mcp.memory.MemoryService;
import com.jasonriddle.mcp.memory.Relation;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for McpMemoryResources.
 */
@QuarkusTest
final class McpMemoryResourcesTest {

    private Path tempMemoryFile;
    private MemoryService memoryService;
    private McpMemoryResources mcpMemoryResources;

    @BeforeEach
    void setUp() throws IOException {
        tempMemoryFile = Files.createTempFile("memory-resources-test", ".jsonl");
        memoryService = new MemoryService(new ObjectMapper(), tempMemoryFile.toString());
        mcpMemoryResources = new McpMemoryResources();
        mcpMemoryResources.memoryService = memoryService;
        mcpMemoryResources.memoryFilePath = tempMemoryFile.toString();

        // Create test data
        final Entity person = new Entity("Jason", "person", List.of("Software developer", "Prefers dark themes"));
        final Entity preferences =
                new Entity("Technical_Preferences", "preferences", List.of("Dark mode", "Vim keybindings"));

        final List<Entity> entities = List.of(person, preferences);
        final List<Relation> relations = List.of(new Relation("Jason", "Technical_Preferences", "has_preferences"));

        // Setup test data in memory service
        memoryService.createEntities(entities);
        memoryService.createRelations(relations);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempMemoryFile != null) {
            Files.deleteIfExists(tempMemoryFile);
        }
    }

    @Test
    void typesResourceShouldListAvailableTypesAndPatterns() {
        final TextResourceContents result = mcpMemoryResources.typesResource();

        assertNotNull(result);
        assertEquals("memory://types", result.uri());
        assertTrue(result.text().contains("Memory Graph Types and Patterns"));
        assertTrue(result.text().contains("**person:** 1 entities"));
        assertTrue(result.text().contains("**preferences:** 1 entities"));
        assertTrue(result.text().contains("**has_preferences:** 1 connections"));
        assertTrue(result.text().contains("Common Patterns"));
    }

    @Test
    void typesResourceShouldHandleEmptyGraph() throws IOException {
        // Create empty memory service
        final Path emptyFile = Files.createTempFile("empty-memory", ".jsonl");
        final MemoryService emptyMemoryService = new MemoryService(new ObjectMapper(), emptyFile.toString());
        mcpMemoryResources.memoryService = emptyMemoryService;

        final TextResourceContents result = mcpMemoryResources.typesResource();

        assertNotNull(result);
        assertTrue(result.text().contains("*No entities found in memory graph.*"));
        assertTrue(result.text().contains("*No relations found in memory graph.*"));

        Files.deleteIfExists(emptyFile);
    }

    @Test
    void memoryStatusResourceShouldProvideComprehensiveStatus() {
        final TextResourceContents result = mcpMemoryResources.memoryStatusResource();

        assertNotNull(result);
        assertEquals("memory://status", result.uri());
        assertTrue(result.text().contains("Memory Graph Status"));
        assertTrue(result.text().contains("**Total Entities:** 2"));
        assertTrue(result.text().contains("**Total Relations:** 1"));
        assertTrue(result.text().contains("**Total Observations:** 4"));
        assertTrue(result.text().contains("**person:** 1"));
        assertTrue(result.text().contains("**preferences:** 1"));
        assertTrue(result.text().contains("**has_preferences:** 1"));
        assertTrue(result.text().contains("**Orphaned Relations:** 0"));
    }

    @Test
    void memoryStatusResourceShouldDetectDataIntegrityIssues() {
        // Create graph with orphaned relation by adding a bad relation
        memoryService.createRelations(List.of(new Relation("Jason", "NonExistentCompany", "works_at")));

        final TextResourceContents result = mcpMemoryResources.memoryStatusResource();

        assertNotNull(result);
        assertTrue(result.text().contains("**Orphaned Relations:** 1"));
        assertTrue(result.text().contains("**Status:** Issues detected - Consider cleanup"));
    }
}
