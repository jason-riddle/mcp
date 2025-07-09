package com.jasonriddle.mcp;

import com.jasonriddle.mcp.memory.Entity;
import com.jasonriddle.mcp.memory.MemoryGraph;
import com.jasonriddle.mcp.memory.MemoryService;
import com.jasonriddle.mcp.memory.Relation;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * MCP memory resources providing memory graph access via memory:// URI scheme.
 */
@ApplicationScoped
public final class McpMemoryResources {

    @Inject
    MemoryService memoryService;

    @Inject
    @ConfigProperty(name = "memory.file.path", defaultValue = "memory.jsonl")
    String memoryFilePath;

    /**
     * Returns types and patterns available in the memory graph.
     *
     * @return types and patterns resource.
     */
    @Resource(uri = "memory://types")
    TextResourceContents typesResource() {
        final MemoryGraph graph = memoryService.readGraph();
        final List<Entity> allEntitiesForCounting = graph.entities();
        final Map<String, Long> entityTypeCounts = new HashMap<>();
        for (final Entity entity : allEntitiesForCounting) {
            final String entityType = entity.entityType();
            entityTypeCounts.merge(entityType, 1L, Long::sum);
        }

        final List<Relation> allRelationsForCounting = graph.relations();
        final Map<String, Long> relationTypeCounts = new HashMap<>();
        for (final Relation relation : allRelationsForCounting) {
            final String relationType = relation.relationType();
            relationTypeCounts.merge(relationType, 1L, Long::sum);
        }

        final StringBuilder content = new StringBuilder();
        content.append("# Memory Graph Types and Patterns\n\n");
        content.append("Overview of available entity types, relation types, and usage patterns.\n\n");

        // Entity Types Section
        content.append("## Entity Types\n\n");
        if (entityTypeCounts.isEmpty()) {
            content.append("*No entities found in memory graph.*\n\n");
        } else {
            content.append("Available entity types and their usage:\n\n");
            for (final Map.Entry<String, Long> entry : entityTypeCounts.entrySet()) {
                content.append("- **")
                        .append(entry.getKey())
                        .append(":** ")
                        .append(entry.getValue())
                        .append(" entities\n");
            }
            content.append("\n");
        }

        // Relation Types Section
        content.append("## Relation Types\n\n");
        if (relationTypeCounts.isEmpty()) {
            content.append("*No relations found in memory graph.*\n\n");
        } else {
            content.append("Available relationship patterns:\n\n");
            for (final Map.Entry<String, Long> entry : relationTypeCounts.entrySet()) {
                content.append("- **")
                        .append(entry.getKey())
                        .append(":** ")
                        .append(entry.getValue())
                        .append(" connections\n");
            }
            content.append("\n");
        }

        // Usage Examples
        content.append("## Common Patterns\n\n");
        if (!entityTypeCounts.isEmpty() && !relationTypeCounts.isEmpty()) {
            content.append("### Entity Naming Examples\n");
            for (final String entityType : entityTypeCounts.keySet()) {
                switch (entityType.toLowerCase()) {
                    case "person" -> content.append("- **person**: Jason, Alice, Bob\n");
                    case "preferences" -> content.append("- **preferences**: Technical_Preferences, UI_Preferences\n");
                    case "project" -> content.append("- **project**: Project_Alpha, Website_Redesign\n");
                    case "system" -> content.append("- **system**: Production_Database, Development_Server\n");
                    default -> content.append("- **")
                            .append(entityType)
                            .append("**: ")
                            .append(entityType)
                            .append("_Example\n");
                }
            }
            content.append("\n### Relationship Examples\n");
            for (final String relationType : relationTypeCounts.keySet()) {
                content.append("- **")
                        .append(relationType)
                        .append("**: Entity_A ")
                        .append(relationType)
                        .append(" Entity_B\n");
            }
        } else {
            content.append("*Create some entities and relationships to see pattern examples.*\n");
        }

        return TextResourceContents.create("memory://types", content.toString());
    }

    /**
     * Returns memory graph status and health information.
     *
     * @return memory status resource.
     */
    @Resource(uri = "memory://status")
    TextResourceContents memoryStatusResource() {
        final MemoryGraph graph = memoryService.readGraph();
        final StringBuilder content = new StringBuilder();

        content.append("# Memory Graph Status\n\n");

        // Basic counts
        content.append("## Overview\n\n");
        content.append("- **Total Entities:** ").append(graph.entities().size()).append("\n");
        content.append("- **Total Relations:** ")
                .append(graph.relations().size())
                .append("\n");

        final List<Entity> allEntities = graph.entities();
        int totalObservations = 0;
        for (Entity e : allEntities) {
            totalObservations += e.observations().size();
        }
        content.append("- **Total Observations:** ").append(totalObservations).append("\n\n");

        // Entity type breakdown
        final List<Entity> entities = graph.entities();
        final Map<String, Long> entityTypeCounts = new HashMap<>();
        for (final Entity entity : entities) {
            final String entityType = entity.entityType();
            entityTypeCounts.merge(entityType, 1L, Long::sum);
        }

        content.append("## Entity Types\n\n");
        if (entityTypeCounts.isEmpty()) {
            content.append("*No entities found.*\n");
        } else {
            for (final Map.Entry<String, Long> entry : entityTypeCounts.entrySet()) {
                content.append("- **")
                        .append(entry.getKey())
                        .append(":** ")
                        .append(entry.getValue())
                        .append("\n");
            }
        }
        content.append("\n");

        // Relation type breakdown
        final List<Relation> relations = graph.relations();
        final Map<String, Long> relationTypeCounts = new HashMap<>();
        for (final Relation relation : relations) {
            final String relationType = relation.relationType();
            relationTypeCounts.merge(relationType, 1L, Long::sum);
        }

        content.append("## Relation Types\n\n");
        if (relationTypeCounts.isEmpty()) {
            content.append("*No relations found.*\n");
        } else {
            for (final Map.Entry<String, Long> entry : relationTypeCounts.entrySet()) {
                content.append("- **")
                        .append(entry.getKey())
                        .append(":** ")
                        .append(entry.getValue())
                        .append("\n");
            }
        }
        content.append("\n");

        // File information
        content.append("## Storage Information\n\n");
        try {
            final Path path = Paths.get(memoryFilePath);
            if (Files.exists(path)) {
                final long fileSize = Files.size(path);
                content.append("- **File Path:** ").append(memoryFilePath).append("\n");
                content.append("- **File Size:** ").append(fileSize).append(" bytes\n");
                content.append("- **Last Modified:** ")
                        .append(Files.getLastModifiedTime(path))
                        .append("\n");
            } else {
                content.append("- **File Path:** ").append(memoryFilePath).append(" (not found)\n");
            }
        } catch (final IOException e) {
            content.append("- **File Path:** ")
                    .append(memoryFilePath)
                    .append(" (error reading: ")
                    .append(e.getMessage())
                    .append(")\n");
        }
        content.append("\n");

        // Data integrity
        content.append("## Data Integrity\n\n");

        // Check for orphaned relations
        final List<Entity> allEntities = graph.entities();
        final Set<String> entityNames = new HashSet<>();
        for (final Entity entity : allEntities) {
            entityNames.add(entity.name());
        }

        final List<Relation> allRelations = graph.relations();
        long orphanedRelations = 0;
        for (final Relation relation : allRelations) {
            final boolean fromExists = entityNames.contains(relation.from());
            final boolean toExists = entityNames.contains(relation.to());
            if (!fromExists || !toExists) {
                orphanedRelations++;
            }
        }

        content.append("- **Orphaned Relations:** ").append(orphanedRelations).append("\n");

        // Check for entities with no observations
        long emptyEntities = 0;
        for (Entity e : graph.entities()) {
            if (e.observations().isEmpty()) {
                emptyEntities++;
            }
        }
        content.append("- **Entities with No Observations:** ")
                .append(emptyEntities)
                .append("\n");

        // Check for isolated entities (no relations)
        final List<Entity> entities = graph.entities();
        final List<Relation> relations = graph.relations();

        // Create sets of entity names that appear in relations for efficient lookup
        final Set<String> entitiesInRelations = new HashSet<>();
        for (final Relation relation : relations) {
            entitiesInRelations.add(relation.from());
            entitiesInRelations.add(relation.to());
        }

        long isolatedEntities = 0;
        for (final Entity entity : entities) {
            final boolean isConnected = entitiesInRelations.contains(entity.name());
            if (!isConnected) {
                isolatedEntities++;
            }
        }
        content.append("- **Isolated Entities:** ").append(isolatedEntities).append("\n");

        content.append("\n## Health Status\n\n");
        if (orphanedRelations == 0 && emptyEntities == 0) {
            content.append("✅ **Status:** Healthy - No data integrity issues detected\n");
        } else {
            content.append("⚠️ **Status:** Issues detected - Consider cleanup\n");
        }

        return TextResourceContents.create("memory://status", content.toString());
    }
}
