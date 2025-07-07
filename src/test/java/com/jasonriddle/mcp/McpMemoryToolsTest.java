package com.jasonriddle.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonriddle.mcp.memory.Entity;
import com.jasonriddle.mcp.memory.MemoryGraph;
import com.jasonriddle.mcp.memory.MemoryService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for McpMemoryTools MCP integration.
 */
final class McpMemoryToolsTest {

    private Path tempMemoryFile;
    private MemoryService memoryService;
    private McpMemoryTools mcpMemoryTools;

    @BeforeEach
    void setUp() throws IOException {
        tempMemoryFile = Files.createTempFile("memory-tools-test", ".jsonl");
        memoryService = new MemoryService(new ObjectMapper(), tempMemoryFile.toString());
        mcpMemoryTools = new McpMemoryTools();
        mcpMemoryTools.memoryService = memoryService;
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempMemoryFile != null) {
            Files.deleteIfExists(tempMemoryFile);
        }
    }

    @Test
    void testCreateEntities() {
        final List<Map<String, Object>> entityData = List.of(
                Map.of(
                        "name",
                        "TestUser",
                        "entityType",
                        "person",
                        "observations",
                        List.of("Test observation 1", "Test observation 2")),
                Map.of(
                        "name",
                        "TestPrefs",
                        "entityType",
                        "preferences",
                        "observations",
                        List.of("Preference observation")));

        final String result = mcpMemoryTools.createEntities(entityData);

        assertTrue(result.contains("Created 2 entities"));

        // Verify entities were created
        final MemoryGraph graph = memoryService.readGraph();
        assertEquals(2, graph.entities().size());
    }

    @Test
    void testCreateEntitiesWithMissingFields() {
        final List<Map<String, Object>> entityData = List.of(
                Map.of("name", "TestUser", "entityType", "person"),
                Map.of("name", "IncompleteEntity") // Missing entityType
                );

        final String result = mcpMemoryTools.createEntities(entityData);

        assertTrue(result.contains("Created 1 entities")); // Only valid entity created

        final MemoryGraph graph = memoryService.readGraph();
        assertEquals(1, graph.entities().size());
        assertEquals("TestUser", graph.entities().get(0).name());
    }

    @Test
    void testCreateRelations() {
        // Create entities first
        final List<Map<String, Object>> entityData = List.of(
                Map.of("name", "TestUser", "entityType", "person", "observations", List.of("Test")),
                Map.of("name", "TestPrefs", "entityType", "preferences", "observations", List.of("Pref")));
        mcpMemoryTools.createEntities(entityData);

        // Create relations
        final List<Map<String, String>> relationData = List.of(
                Map.of("from", "TestUser", "to", "TestPrefs", "relationType", "has_preferences"),
                Map.of("from", "TestUser", "to", "TestPrefs", "relationType", "uses_settings"));

        final String result = mcpMemoryTools.createRelations(relationData);

        assertTrue(result.contains("Created 2 relations"));

        // Verify relations were created
        final MemoryGraph graph = memoryService.readGraph();
        assertEquals(2, graph.relations().size());
    }

    @Test
    void testCreateRelationsWithMissingFields() {
        // Create entities first so relations can be created
        final List<Map<String, Object>> entityData = List.of(
                Map.of("name", "TestUser", "entityType", "person", "observations", List.of("Test")),
                Map.of("name", "TestPrefs", "entityType", "preferences", "observations", List.of("Pref")));
        mcpMemoryTools.createEntities(entityData);

        final List<Map<String, String>> relationData = List.of(
                Map.of("from", "TestUser", "to", "TestPrefs", "relationType", "has_preferences"),
                Map.of("from", "TestUser", "to", "TestPrefs") // Missing relationType
                );

        final String result = mcpMemoryTools.createRelations(relationData);

        assertTrue(result.contains("Created 1 relations")); // Only 1 valid relation created
    }

    @Test
    void testAddObservations() {
        // Create entity first
        final List<Map<String, Object>> entityData = List.of(
                Map.of("name", "TestUser", "entityType", "person", "observations", List.of("Original observation")));
        mcpMemoryTools.createEntities(entityData);

        // Add observations
        final List<Map<String, Object>> observationData = List.of(
                Map.of("entityName", "TestUser", "contents", List.of("New observation 1", "New observation 2")));

        final String result = mcpMemoryTools.addObservations(observationData);

        assertTrue(result.contains("Added 2 observations to 1 entities"));

        // Verify observations were added
        final MemoryGraph graph = memoryService.readGraph();
        final Entity entity = graph.entities().get(0);
        assertEquals(3, entity.observations().size()); // Original + 2 new
    }

    @Test
    void testAddObservationsToNonExistentEntity() {
        final List<Map<String, Object>> observationData =
                List.of(Map.of("entityName", "NonExistent", "contents", List.of("New observation")));

        final String result = mcpMemoryTools.addObservations(observationData);

        assertTrue(result.contains("Added 0 observations to 0 entities"));
    }

    @Test
    void testDeleteEntities() {
        // Create entities first
        final List<Map<String, Object>> entityData = List.of(
                Map.of("name", "TestUser1", "entityType", "person", "observations", List.of("Test1")),
                Map.of("name", "TestUser2", "entityType", "person", "observations", List.of("Test2")));
        mcpMemoryTools.createEntities(entityData);

        // Delete one entity
        final String result = mcpMemoryTools.deleteEntities(List.of("TestUser1"));

        assertTrue(result.contains("Deleted 1 entities"));

        // Verify entity was deleted
        final MemoryGraph graph = memoryService.readGraph();
        assertEquals(1, graph.entities().size());
        assertEquals("TestUser2", graph.entities().get(0).name());
    }

    @Test
    void testDeleteObservations() {
        // Create entity first
        final List<Map<String, Object>> entityData = List.of(Map.of(
                "name",
                "TestUser",
                "entityType",
                "person",
                "observations",
                List.of("Keep this", "Delete this", "Also delete")));
        mcpMemoryTools.createEntities(entityData);

        // Delete specific observations
        final List<Map<String, Object>> deletionData =
                List.of(Map.of("entityName", "TestUser", "observations", List.of("Delete this", "Also delete")));

        final String result = mcpMemoryTools.deleteObservations(deletionData);

        assertTrue(result.contains("Deleted 2 observations from 1 entities"));

        // Verify observations were deleted
        final MemoryGraph graph = memoryService.readGraph();
        final Entity entity = graph.entities().get(0);
        assertEquals(1, entity.observations().size());
        assertEquals("Keep this", entity.observations().get(0));
    }

    @Test
    void testDeleteRelations() {
        // Create entities and relations first
        final List<Map<String, Object>> entityData = List.of(
                Map.of("name", "TestUser", "entityType", "person", "observations", List.of("Test")),
                Map.of("name", "TestPrefs", "entityType", "preferences", "observations", List.of("Pref")));
        mcpMemoryTools.createEntities(entityData);

        final List<Map<String, String>> relationData = List.of(
                Map.of("from", "TestUser", "to", "TestPrefs", "relationType", "has_preferences"),
                Map.of("from", "TestUser", "to", "TestPrefs", "relationType", "uses_settings"));
        mcpMemoryTools.createRelations(relationData);

        // Delete one relation
        final List<Map<String, String>> deleteData =
                List.of(Map.of("from", "TestUser", "to", "TestPrefs", "relationType", "has_preferences"));

        final String result = mcpMemoryTools.deleteRelations(deleteData);

        assertTrue(result.contains("Deleted 1 relations"));

        // Verify relation was deleted
        final MemoryGraph graph = memoryService.readGraph();
        assertEquals(1, graph.relations().size());
        assertEquals("uses_settings", graph.relations().get(0).relationType());
    }

    @Test
    void testReadGraph() {
        // Create test data
        final List<Map<String, Object>> entityData =
                List.of(Map.of("name", "TestUser", "entityType", "person", "observations", List.of("Test")));
        mcpMemoryTools.createEntities(entityData);

        final MemoryGraph result = mcpMemoryTools.readGraph();

        assertNotNull(result);
        assertEquals(1, result.entities().size());
        assertEquals("TestUser", result.entities().get(0).name());
    }

    @Test
    void testSearchNodes() {
        // Create test data
        final List<Map<String, Object>> entityData = List.of(
                Map.of(
                        "name",
                        "TestUser",
                        "entityType",
                        "person",
                        "observations",
                        List.of("Likes testing", "Uses Java")),
                Map.of(
                        "name",
                        "OtherUser",
                        "entityType",
                        "person",
                        "observations",
                        List.of("Likes Python", "Dislikes testing")));
        mcpMemoryTools.createEntities(entityData);

        // Search for "testing"
        final MemoryGraph result = mcpMemoryTools.searchNodes("testing");

        assertNotNull(result);
        assertEquals(2, result.entities().size()); // Both mention testing

        // Search for "Java"
        final MemoryGraph javaResult = mcpMemoryTools.searchNodes("Java");
        assertEquals(1, javaResult.entities().size());
        assertEquals("TestUser", javaResult.entities().get(0).name());

        // Search for non-existent term
        final MemoryGraph emptyResult = mcpMemoryTools.searchNodes("nonexistent");
        assertEquals(0, emptyResult.entities().size());
    }

    @Test
    void testOpenNodes() {
        // Create test data
        final List<Map<String, Object>> entityData = List.of(
                Map.of("name", "TestUser", "entityType", "person", "observations", List.of("Test")),
                Map.of("name", "TestPrefs", "entityType", "preferences", "observations", List.of("Pref")));
        mcpMemoryTools.createEntities(entityData);

        final List<Map<String, String>> relationData =
                List.of(Map.of("from", "TestUser", "to", "TestPrefs", "relationType", "has_preferences"));
        mcpMemoryTools.createRelations(relationData);

        // Open specific nodes
        final MemoryGraph result = mcpMemoryTools.openNodes(List.of("TestUser", "TestPrefs"));

        assertNotNull(result);
        assertEquals(2, result.entities().size());
        assertEquals(1, result.relations().size());

        // Open single node
        final MemoryGraph singleResult = mcpMemoryTools.openNodes(List.of("TestUser"));
        assertEquals(1, singleResult.entities().size());
        assertEquals("TestUser", singleResult.entities().get(0).name());

        // Open non-existent node
        final MemoryGraph emptyResult = mcpMemoryTools.openNodes(List.of("NonExistent"));
        assertEquals(0, emptyResult.entities().size());
    }

    @Test
    void testIntegratedWorkflow() {
        // Test a complete workflow: create entities, add relations, add observations, search, cleanup

        // 1. Create entities
        final List<Map<String, Object>> entityData = List.of(
                Map.of("name", "Jason", "entityType", "person", "observations", List.of("User's name is Jason")),
                Map.of(
                        "name",
                        "Technical_Preferences",
                        "entityType",
                        "preferences",
                        "observations",
                        List.of("Uses Fastmail for email", "Prefers absolute paths")));
        mcpMemoryTools.createEntities(entityData);

        // 2. Create relation
        final List<Map<String, String>> relationData =
                List.of(Map.of("from", "Jason", "to", "Technical_Preferences", "relationType", "has_preferences"));
        mcpMemoryTools.createRelations(relationData);

        // 3. Add more observations
        final List<Map<String, Object>> additionalObs = List.of(
                Map.of("entityName", "Technical_Preferences", "contents", List.of("Uses NextCloud for file storage")));
        mcpMemoryTools.addObservations(additionalObs);

        // 4. Verify full graph
        final MemoryGraph fullGraph = mcpMemoryTools.readGraph();
        assertEquals(2, fullGraph.entities().size());
        assertEquals(1, fullGraph.relations().size());

        // 5. Search for specific content
        final MemoryGraph searchResult = mcpMemoryTools.searchNodes("Fastmail");
        assertEquals(1, searchResult.entities().size());
        assertEquals("Technical_Preferences", searchResult.entities().get(0).name());

        // 6. Open specific nodes
        final MemoryGraph openResult = mcpMemoryTools.openNodes(List.of("Jason"));
        assertEquals(1, openResult.entities().size());
        assertEquals("Jason", openResult.entities().get(0).name());

        // Verify Technical_Preferences has 3 observations now
        final Entity techPrefs = fullGraph.entities().stream()
                .filter(e -> "Technical_Preferences".equals(e.name()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, techPrefs.observations().size());
    }
}
