package com.jasonriddle.mcp.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for MemoryService.
 */
@QuarkusTest
class MemoryServiceTest {

    private Path tempMemoryFile;
    private MemoryService memoryService;

    @BeforeEach
    void setUp() throws IOException {
        tempMemoryFile = Files.createTempFile("test-memory", ".jsonl");
        memoryService = new MemoryService(new ObjectMapper(), tempMemoryFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempMemoryFile);
    }

    @Test
    void testCreateAndReadEntities() {
        // Create entities
        Entity testEntity = new Entity("TestUser", "person", List.of("Likes testing", "Uses Java"));
        List<Entity> created = memoryService.createEntities(List.of(testEntity));

        assertEquals(1, created.size());
        assertEquals("TestUser", created.get(0).name());

        // Read back
        MemoryGraph graph = memoryService.readGraph();
        assertEquals(1, graph.entities().size());
        assertEquals("TestUser", graph.entities().get(0).name());
        assertEquals("person", graph.entities().get(0).entityType());
        assertEquals(2, graph.entities().get(0).observations().size());
    }

    @Test
    void testCreateAndReadRelations() {
        // Create entities first
        Entity user = new Entity("TestUser", "person", List.of("Test observation"));
        Entity preferences = new Entity("TestPreferences", "preferences", List.of("Test preference"));
        memoryService.createEntities(List.of(user, preferences));

        // Create relation
        Relation testRelation = new Relation("TestUser", "TestPreferences", "has_preferences");
        List<Relation> created = memoryService.createRelations(List.of(testRelation));

        assertEquals(1, created.size());
        assertEquals("TestUser", created.get(0).from());
        assertEquals("TestPreferences", created.get(0).to());
        assertEquals("has_preferences", created.get(0).relationType());

        // Read back
        MemoryGraph graph = memoryService.readGraph();
        assertEquals(2, graph.entities().size());
        assertEquals(1, graph.relations().size());
    }

    @Test
    void testSearchNodes() {
        // Create test data
        Entity user = new Entity("TestUser", "person", List.of("Likes testing", "Uses Java"));
        memoryService.createEntities(List.of(user));

        // Search
        MemoryGraph results = memoryService.searchNodes("testing");
        assertEquals(1, results.entities().size());
        assertEquals("TestUser", results.entities().get(0).name());

        // Search for non-existent
        MemoryGraph empty = memoryService.searchNodes("nonexistent");
        assertTrue(empty.entities().isEmpty());
    }

    @Test
    void testAddObservations() {
        // Create entity
        Entity user = new Entity("TestUser", "person", List.of("Initial observation"));
        memoryService.createEntities(List.of(user));

        // Add observations
        var observationMap = java.util.Map.of("TestUser", List.of("New observation", "Another observation"));
        var added = memoryService.addObservations(observationMap);

        assertEquals(1, added.size());
        assertEquals(2, added.get("TestUser").size());

        // Verify
        MemoryGraph graph = memoryService.readGraph();
        assertEquals(3, graph.entities().get(0).observations().size());
    }

    @Test
    void testEmptyGraph() {
        MemoryGraph graph = memoryService.readGraph();
        assertNotNull(graph);
        assertTrue(graph.entities().isEmpty());
        assertTrue(graph.relations().isEmpty());
    }
}
