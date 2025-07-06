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
import java.util.Map;
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

    @Test
    void testDeleteEntities() {
        // Create entities
        Entity user = new Entity("TestUser", "person", List.of("Likes testing"));
        Entity preferences = new Entity("TestPreferences", "preferences", List.of("Test preference"));
        memoryService.createEntities(List.of(user, preferences));

        // Create relation
        Relation testRelation = new Relation("TestUser", "TestPreferences", "has_preferences");
        memoryService.createRelations(List.of(testRelation));

        // Verify initial state
        MemoryGraph initialGraph = memoryService.readGraph();
        assertEquals(2, initialGraph.entities().size());
        assertEquals(1, initialGraph.relations().size());

        // Delete one entity
        List<String> deleted = memoryService.deleteEntities(List.of("TestUser"));
        assertEquals(1, deleted.size());
        assertEquals("TestUser", deleted.get(0));

        // Verify entity and related relations are deleted
        MemoryGraph afterDelete = memoryService.readGraph();
        assertEquals(1, afterDelete.entities().size());
        assertEquals("TestPreferences", afterDelete.entities().get(0).name());
        assertTrue(afterDelete.relations().isEmpty()); // Relations should be deleted too
    }

    @Test
    void testDeleteMultipleEntities() {
        // Create entities
        Entity user1 = new Entity("User1", "person", List.of("Test 1"));
        Entity user2 = new Entity("User2", "person", List.of("Test 2"));
        Entity user3 = new Entity("User3", "person", List.of("Test 3"));
        memoryService.createEntities(List.of(user1, user2, user3));

        // Delete multiple entities
        List<String> deleted = memoryService.deleteEntities(List.of("User1", "User3"));
        assertEquals(2, deleted.size());
        assertTrue(deleted.contains("User1"));
        assertTrue(deleted.contains("User3"));

        // Verify only User2 remains
        MemoryGraph afterDelete = memoryService.readGraph();
        assertEquals(1, afterDelete.entities().size());
        assertEquals("User2", afterDelete.entities().get(0).name());
    }

    @Test
    void testDeleteNonExistentEntities() {
        // Create one entity
        Entity user = new Entity("TestUser", "person", List.of("Test"));
        memoryService.createEntities(List.of(user));

        // Try to delete non-existent entity
        List<String> deleted = memoryService.deleteEntities(List.of("NonExistent"));
        assertEquals(1, deleted.size());
        assertEquals("NonExistent", deleted.get(0));

        // Verify original entity still exists
        MemoryGraph afterDelete = memoryService.readGraph();
        assertEquals(1, afterDelete.entities().size());
        assertEquals("TestUser", afterDelete.entities().get(0).name());
    }

    @Test
    void testDeleteObservations() {
        // Create entity with observations
        Entity user = new Entity("TestUser", "person", List.of("Observation 1", "Observation 2", "Observation 3"));
        memoryService.createEntities(List.of(user));

        // Delete specific observations
        Map<String, List<String>> deletionMap = Map.of("TestUser", List.of("Observation 1", "Observation 3"));
        Map<String, List<String>> deleted = memoryService.deleteObservations(deletionMap);

        // Verify return value
        assertEquals(1, deleted.size());
        assertTrue(deleted.containsKey("TestUser"));
        assertEquals(2, deleted.get("TestUser").size());
        assertTrue(deleted.get("TestUser").contains("Observation 1"));
        assertTrue(deleted.get("TestUser").contains("Observation 3"));

        // Verify entity state
        MemoryGraph afterDelete = memoryService.readGraph();
        assertEquals(1, afterDelete.entities().size());
        Entity updatedUser = afterDelete.entities().get(0);
        assertEquals(1, updatedUser.observations().size());
        assertEquals("Observation 2", updatedUser.observations().get(0));
    }

    @Test
    void testDeleteNonExistentObservations() {
        // Create entity with observations
        Entity user = new Entity("TestUser", "person", List.of("Observation 1", "Observation 2"));
        memoryService.createEntities(List.of(user));

        // Try to delete non-existent observations
        Map<String, List<String>> deletionMap = Map.of("TestUser", List.of("Non-existent", "Observation 1"));
        Map<String, List<String>> deleted = memoryService.deleteObservations(deletionMap);

        // Verify only existing observation was deleted
        assertEquals(1, deleted.size());
        assertTrue(deleted.containsKey("TestUser"));
        assertEquals(1, deleted.get("TestUser").size());
        assertEquals("Observation 1", deleted.get("TestUser").get(0));

        // Verify entity state
        MemoryGraph afterDelete = memoryService.readGraph();
        Entity updatedUser = afterDelete.entities().get(0);
        assertEquals(1, updatedUser.observations().size());
        assertEquals("Observation 2", updatedUser.observations().get(0));
    }

    @Test
    void testDeleteObservationsFromNonExistentEntity() {
        // Create entity
        Entity user = new Entity("TestUser", "person", List.of("Observation 1"));
        memoryService.createEntities(List.of(user));

        // Try to delete observations from non-existent entity
        Map<String, List<String>> deletionMap = Map.of("NonExistentUser", List.of("Observation 1"));
        Map<String, List<String>> deleted = memoryService.deleteObservations(deletionMap);

        // Verify nothing was deleted
        assertTrue(deleted.isEmpty());

        // Verify original entity unchanged
        MemoryGraph afterDelete = memoryService.readGraph();
        assertEquals(1, afterDelete.entities().size());
        Entity originalUser = afterDelete.entities().get(0);
        assertEquals(1, originalUser.observations().size());
        assertEquals("Observation 1", originalUser.observations().get(0));
    }

    @Test
    void testDeleteRelations() {
        // Create entities
        Entity user = new Entity("TestUser", "person", List.of("Test"));
        Entity preferences = new Entity("TestPreferences", "preferences", List.of("Test"));
        Entity settings = new Entity("TestSettings", "settings", List.of("Test"));
        memoryService.createEntities(List.of(user, preferences, settings));

        // Create relations
        Relation relation1 = new Relation("TestUser", "TestPreferences", "has_preferences");
        Relation relation2 = new Relation("TestUser", "TestSettings", "has_settings");
        Relation relation3 = new Relation("TestPreferences", "TestSettings", "related_to");
        memoryService.createRelations(List.of(relation1, relation2, relation3));

        // Verify initial state
        MemoryGraph initialGraph = memoryService.readGraph();
        assertEquals(3, initialGraph.entities().size());
        assertEquals(3, initialGraph.relations().size());

        // Delete specific relations
        List<Relation> toDelete = List.of(relation1, relation3);
        List<Relation> deleted = memoryService.deleteRelations(toDelete);

        // Verify return value
        assertEquals(2, deleted.size());
        assertTrue(deleted.contains(relation1));
        assertTrue(deleted.contains(relation3));

        // Verify graph state
        MemoryGraph afterDelete = memoryService.readGraph();
        assertEquals(3, afterDelete.entities().size()); // Entities should remain
        assertEquals(1, afterDelete.relations().size());
        assertEquals(relation2, afterDelete.relations().get(0));
    }

    @Test
    void testDeleteNonExistentRelations() {
        // Create entities and one relation
        Entity user = new Entity("TestUser", "person", List.of("Test"));
        Entity preferences = new Entity("TestPreferences", "preferences", List.of("Test"));
        memoryService.createEntities(List.of(user, preferences));

        Relation existingRelation = new Relation("TestUser", "TestPreferences", "has_preferences");
        memoryService.createRelations(List.of(existingRelation));

        // Try to delete non-existent relation
        Relation nonExistentRelation = new Relation("TestUser", "TestPreferences", "non_existent_type");
        List<Relation> deleted = memoryService.deleteRelations(List.of(nonExistentRelation));

        // Verify return value (method returns what was requested to delete)
        assertEquals(1, deleted.size());
        assertEquals(nonExistentRelation, deleted.get(0));

        // Verify original relation still exists
        MemoryGraph afterDelete = memoryService.readGraph();
        assertEquals(1, afterDelete.relations().size());
        assertEquals(existingRelation, afterDelete.relations().get(0));
    }

    @Test
    void testDeleteAllRelations() {
        // Create entities
        Entity user = new Entity("TestUser", "person", List.of("Test"));
        Entity preferences = new Entity("TestPreferences", "preferences", List.of("Test"));
        memoryService.createEntities(List.of(user, preferences));

        // Create relations
        Relation relation1 = new Relation("TestUser", "TestPreferences", "has_preferences");
        Relation relation2 = new Relation("TestPreferences", "TestUser", "belongs_to");
        memoryService.createRelations(List.of(relation1, relation2));

        // Delete all relations
        List<Relation> allRelations = List.of(relation1, relation2);
        List<Relation> deleted = memoryService.deleteRelations(allRelations);

        // Verify all relations deleted
        assertEquals(2, deleted.size());
        assertTrue(deleted.contains(relation1));
        assertTrue(deleted.contains(relation2));

        // Verify graph state
        MemoryGraph afterDelete = memoryService.readGraph();
        assertEquals(2, afterDelete.entities().size()); // Entities should remain
        assertTrue(afterDelete.relations().isEmpty());
    }
}
