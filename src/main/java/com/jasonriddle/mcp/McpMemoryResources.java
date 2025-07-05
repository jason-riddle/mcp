package com.jasonriddle.mcp;

import com.jasonriddle.mcp.memory.Entity;
import com.jasonriddle.mcp.memory.MemoryGraph;
import com.jasonriddle.mcp.memory.MemoryService;
import com.jasonriddle.mcp.memory.Relation;
import io.quarkiverse.mcp.server.McpServer;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * MCP memory resources providing memory graph access via memory:// URI scheme.
 */
@ApplicationScoped
@McpServer("memory")
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
        final Map<String, Long> entityTypeCounts =
                graph.entities().stream().collect(Collectors.groupingBy(Entity::entityType, Collectors.counting()));
        final Map<String, Long> relationTypeCounts = graph.relations().stream()
                .collect(Collectors.groupingBy(Relation::relationType, Collectors.counting()));

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
        content.append("## Overview\n");
        content.append("- **Total Entities:** ").append(graph.entities().size()).append("\n");
        content.append("- **Total Relations:** ")
                .append(graph.relations().size())
                .append("\n");

        final int totalObservations =
                graph.entities().stream().mapToInt(e -> e.observations().size()).sum();
        content.append("- **Total Observations:** ").append(totalObservations).append("\n\n");

        // Entity type breakdown
        final Map<String, Long> entityTypeCounts =
                graph.entities().stream().collect(Collectors.groupingBy(Entity::entityType, Collectors.counting()));

        content.append("## Entity Types\n");
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
        final Map<String, Long> relationTypeCounts = graph.relations().stream()
                .collect(Collectors.groupingBy(Relation::relationType, Collectors.counting()));

        content.append("## Relation Types\n");
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
        content.append("## Storage Information\n");
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
        content.append("## Data Integrity\n");

        // Check for orphaned relations
        final List<String> entityNames =
                graph.entities().stream().map(Entity::name).collect(Collectors.toList());

        final long orphanedRelations = graph.relations().stream()
                .filter(r -> !entityNames.contains(r.from()) || !entityNames.contains(r.to()))
                .count();

        content.append("- **Orphaned Relations:** ").append(orphanedRelations).append("\n");

        // Check for entities with no observations
        final long emptyEntities = graph.entities().stream()
                .filter(e -> e.observations().isEmpty())
                .count();
        content.append("- **Entities with No Observations:** ")
                .append(emptyEntities)
                .append("\n");

        // Check for isolated entities (no relations)
        final long isolatedEntities = graph.entities().stream()
                .filter(e ->
                        graph.relations().stream().noneMatch(r -> r.from().equals(e.name()) || r.to().equals(e.name())))
                .count();
        content.append("- **Isolated Entities:** ").append(isolatedEntities).append("\n");

        content.append("\n## Health Status\n");
        if (orphanedRelations == 0 && emptyEntities == 0) {
            content.append("✅ **Status:** Healthy - No data integrity issues detected\n");
        } else {
            content.append("⚠️ **Status:** Issues detected - Consider cleanup\n");
        }

        return TextResourceContents.create("memory://status", content.toString());
    }
}
