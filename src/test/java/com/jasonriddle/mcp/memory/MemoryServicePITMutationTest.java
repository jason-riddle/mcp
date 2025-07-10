package com.jasonriddle.mcp.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Standalone tests for MemoryService designed for PITest mutation testing.
 * These tests do not use @QuarkusTest to avoid dependency injection issues with PITest.
 */
class MemoryServicePITMutationTest {

    private Path tempMemoryFile;
    private MemoryService memoryService;

    @BeforeEach
    void setUp() throws IOException {
        tempMemoryFile = Files.createTempFile("memory-service-pit-test", ".jsonl");
        memoryService = new MemoryService(new ObjectMapper(), tempMemoryFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempMemoryFile);
    }

    @Test
    void testCreateAndReadEntities() {
        // Create entities
        final Entity entity1 = new Entity("person1", "Person", List.of("A software developer"));
        final Entity entity2 = new Entity("person2", "Person", List.of("A product manager"));

        memoryService.createEntities(List.of(entity1, entity2));

        // Read entities
        final MemoryGraph graph = memoryService.readGraph();
        assertEquals(2, graph.entities().size());

        boolean foundPerson1 = false;
        boolean foundPerson2 = false;
        for (Entity entity : graph.entities()) {
            if (entity.name().equals("person1")) {
                foundPerson1 = true;
            }
            if (entity.name().equals("person2")) {
                foundPerson2 = true;
            }
        }
        assertTrue(foundPerson1);
        assertTrue(foundPerson2);
    }

    @Test
    void testCreateAndReadRelations() {
        // Create entities first
        final Entity entity1 = new Entity("person1", "Person", List.of("A software developer"));
        final Entity entity2 = new Entity("person2", "Person", List.of("A product manager"));
        memoryService.createEntities(List.of(entity1, entity2));

        // Create relation
        final Relation relation = new Relation("person1", "person2", "works_with");
        memoryService.createRelations(List.of(relation));

        // Read relations
        final MemoryGraph graph = memoryService.readGraph();
        assertEquals(1, graph.relations().size());
        assertEquals("works_with", graph.relations().get(0).relationType());
    }

    @Test
    void testAddObservations() {
        // Create entity
        final Entity entity = new Entity("person1", "Person", List.of("A software developer"));
        memoryService.createEntities(List.of(entity));

        // Add observations
        memoryService.addObservations(Map.of("person1", List.of("Loves coffee", "Works remotely")));

        // Verify observations were added
        final MemoryGraph graph = memoryService.readGraph();
        Entity updatedEntity = null;
        for (Entity e : graph.entities()) {
            if (e.name().equals("person1")) {
                updatedEntity = e;
                break;
            }
        }
        assertNotNull(updatedEntity);
        assertEquals(3, updatedEntity.observations().size());
        assertTrue(updatedEntity.observations().contains("Loves coffee"));
        assertTrue(updatedEntity.observations().contains("Works remotely"));
    }

    @Test
    void testReadMemoryGraph() {
        // Create entities and relations
        final Entity entity1 = new Entity("person1", "Person", List.of("A software developer"));
        final Entity entity2 = new Entity("person2", "Person", List.of("A product manager"));
        memoryService.createEntities(List.of(entity1, entity2));

        final Relation relation = new Relation("person1", "person2", "works_with");
        memoryService.createRelations(List.of(relation));

        // Read full graph
        final MemoryGraph graph = memoryService.readGraph();
        assertNotNull(graph);
        assertEquals(2, graph.entities().size());
        assertEquals(1, graph.relations().size());
    }

    @Test
    void testDeleteEntities() {
        // Create entities
        final Entity entity1 = new Entity("person1", "Person", List.of("A software developer"));
        final Entity entity2 = new Entity("person2", "Person", List.of("A product manager"));
        memoryService.createEntities(List.of(entity1, entity2));

        // Delete one entity
        memoryService.deleteEntities(List.of("person1"));

        // Verify deletion
        final MemoryGraph graph = memoryService.readGraph();
        assertEquals(1, graph.entities().size());

        boolean foundPerson2 = false;
        for (Entity entity : graph.entities()) {
            if (entity.name().equals("person2")) {
                foundPerson2 = true;
                break;
            }
        }
        assertTrue(foundPerson2);
    }

    @Test
    void testDeleteRelations() {
        // Create entities and relation
        final Entity entity1 = new Entity("person1", "Person", List.of("A software developer"));
        final Entity entity2 = new Entity("person2", "Person", List.of("A product manager"));
        memoryService.createEntities(List.of(entity1, entity2));

        final Relation relation = new Relation("person1", "person2", "works_with");
        memoryService.createRelations(List.of(relation));

        // Delete relation
        memoryService.deleteRelations(List.of(relation));

        // Verify deletion
        final MemoryGraph graph = memoryService.readGraph();
        assertEquals(0, graph.relations().size());
    }

    @Test
    void testSearchNodes() {
        // Create entities with different observations
        final Entity entity1 = new Entity("person1", "Person", List.of("Java developer", "Loves coffee"));
        final Entity entity2 = new Entity("person2", "Person", List.of("Python developer", "Loves tea"));
        memoryService.createEntities(List.of(entity1, entity2));

        // Search for Java-related entities
        final MemoryGraph javaResults = memoryService.searchNodes("Java");
        assertEquals(1, javaResults.entities().size());
        assertEquals("person1", javaResults.entities().get(0).name());

        // Search for developer-related entities
        final MemoryGraph developerResults = memoryService.searchNodes("developer");
        assertEquals(2, developerResults.entities().size());
    }
}
