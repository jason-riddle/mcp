package com.jasonriddle.mcp;

import com.jasonriddle.mcp.memory.Entity;
import com.jasonriddle.mcp.memory.MemoryGraph;
import com.jasonriddle.mcp.memory.MemoryService;
import com.jasonriddle.mcp.memory.Relation;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * MCP memory tools for knowledge graph operations.
 */
@ApplicationScoped
public final class McpMemoryTools {

    @Inject
    MemoryService memoryService;

    /**
     * Creates multiple new entities in the knowledge graph.
     *
     * @param entities entities to create.
     * @return created entities as JSON string.
     */
    @Tool(name = "memory.create_entities", description = "Create multiple new entities in the knowledge graph")
    public String createEntities(
            @ToolArg(description = "Array of entities with name, entityType, and observations")
                    final List<Map<String, Object>> entities) {
        final List<Entity> entitiesToCreate = new java.util.ArrayList<>();

        for (final Map<String, Object> entityData : entities) {
            final String name = (String) entityData.get("name");
            final String entityType = (String) entityData.get("entityType");
            @SuppressWarnings("unchecked")
            final List<String> observations = (List<String>) entityData.get("observations");

            if (name != null && entityType != null) {
                final Entity entity;
                if (observations != null) {
                    entity = new Entity(name, entityType, observations);
                } else {
                    entity = new Entity(name, entityType, List.of());
                }
                entitiesToCreate.add(entity);
            }
        }

        final List<Entity> created = memoryService.createEntities(entitiesToCreate);
        return "Created " + created.size() + " entities";
    }

    /**
     * Creates multiple new relations between entities.
     *
     * @param relations relations to create.
     * @return created relations as JSON string.
     */
    @Tool(name = "memory.create_relations", description = "Create multiple new relations between entities")
    public String createRelations(
            @ToolArg(description = "Array of relations with from, to, and relationType")
                    final List<Map<String, String>> relations) {
        final List<Relation> relationsToCreate = new java.util.ArrayList<>();

        for (final Map<String, String> relationData : relations) {
            final String from = relationData.get("from");
            final String to = relationData.get("to");
            final String relationType = relationData.get("relationType");

            final boolean hasFrom = from != null;
            final boolean hasTo = to != null;
            final boolean hasRelationType = relationType != null;
            final boolean hasAllFields = hasFrom && hasTo;
            if (hasAllFields && hasRelationType) {
                final Relation relation = new Relation(from, to, relationType);
                relationsToCreate.add(relation);
            }
        }

        final List<Relation> created = memoryService.createRelations(relationsToCreate);
        return "Created " + created.size() + " relations";
    }

    /**
     * Adds observations to existing entities.
     *
     * @param observations observations to add.
     * @return added observations as JSON string.
     */
    @Tool(name = "memory.add_observations", description = "Add new observations to existing entities")
    public String addObservations(
            @ToolArg(description = "Array of objects with entityName and contents")
                    final List<Map<String, Object>> observations) {
        final Map<String, List<String>> observationMap = new java.util.HashMap<>();

        for (final Map<String, Object> obsData : observations) {
            final String entityName = (String) obsData.get("entityName");
            @SuppressWarnings("unchecked")
            final List<String> contents = (List<String>) obsData.get("contents");

            if (entityName != null && contents != null) {
                observationMap.put(entityName, contents);
            }
        }

        final Map<String, List<String>> added = memoryService.addObservations(observationMap);
        final Collection<List<String>> observationLists = added.values();
        int totalAdded = 0;
        for (List<String> observationList : observationLists) {
            totalAdded += observationList.size();
        }
        return "Added " + totalAdded + " observations to " + added.size() + " entities";
    }

    /**
     * Deletes entities and their relations.
     *
     * @param entityNames names of entities to delete.
     * @return deleted entity names as JSON string.
     */
    @Tool(name = "memory.delete_entities", description = "Remove entities and their relations from the knowledge graph")
    public String deleteEntities(
            @ToolArg(description = "Array of entity names to delete") final List<String> entityNames) {
        final List<String> deleted = memoryService.deleteEntities(entityNames);
        return "Deleted " + deleted.size() + " entities";
    }

    /**
     * Deletes specific observations from entities.
     *
     * @param deletions deletions to perform.
     * @return deleted observations as JSON string.
     */
    @Tool(name = "memory.delete_observations", description = "Remove specific observations from entities")
    public String deleteObservations(
            @ToolArg(description = "Array of objects with entityName and observations to delete")
                    final List<Map<String, Object>> deletions) {
        final Map<String, List<String>> deletionMap = new java.util.HashMap<>();

        for (final Map<String, Object> delData : deletions) {
            final String entityName = (String) delData.get("entityName");
            @SuppressWarnings("unchecked")
            final List<String> observations = (List<String>) delData.get("observations");

            if (entityName != null && observations != null) {
                deletionMap.put(entityName, observations);
            }
        }

        final Map<String, List<String>> deleted = memoryService.deleteObservations(deletionMap);
        final Collection<List<String>> deletionLists = deleted.values();
        int totalDeleted = 0;
        for (List<String> deletionList : deletionLists) {
            totalDeleted += deletionList.size();
        }
        return "Deleted " + totalDeleted + " observations from " + deleted.size() + " entities";
    }

    /**
     * Deletes specific relations.
     *
     * @param relations relations to delete.
     * @return deleted relations as JSON string.
     */
    @Tool(name = "memory.delete_relations", description = "Remove specific relations from the knowledge graph")
    public String deleteRelations(
            @ToolArg(description = "Array of relations with from, to, and relationType")
                    final List<Map<String, String>> relations) {
        final List<Relation> relationsToDelete = new java.util.ArrayList<>();

        for (final Map<String, String> relationData : relations) {
            final String from = relationData.get("from");
            final String to = relationData.get("to");
            final String relationType = relationData.get("relationType");

            final boolean hasFrom = from != null;
            final boolean hasTo = to != null;
            final boolean hasRelationType = relationType != null;
            final boolean hasAllFields = hasFrom && hasTo;
            if (hasAllFields && hasRelationType) {
                final Relation relation = new Relation(from, to, relationType);
                relationsToDelete.add(relation);
            }
        }

        final List<Relation> deleted = memoryService.deleteRelations(relationsToDelete);
        return "Deleted " + deleted.size() + " relations";
    }

    /**
     * Reads the entire knowledge graph.
     *
     * @return complete graph structure as JSON string.
     */
    @Tool(name = "memory.read_graph", description = "Read the entire knowledge graph")
    public MemoryGraph readGraph() {
        return memoryService.readGraph();
    }

    /**
     * Searches for nodes based on query.
     *
     * @param query search query.
     * @return matching entities and relations as JSON string.
     */
    @Tool(name = "memory.search_nodes", description = "Search for nodes in the knowledge graph based on a query")
    public MemoryGraph searchNodes(
            @ToolArg(description = "The search query to match against entity names, types, and observation content")
                    final String query) {
        return memoryService.searchNodes(query);
    }

    /**
     * Opens specific nodes by name.
     *
     * @param names entity names to retrieve.
     * @return requested entities and relations as JSON string.
     */
    @Tool(name = "memory.open_nodes", description = "Retrieve specific nodes by name")
    public MemoryGraph openNodes(@ToolArg(description = "Array of entity names to retrieve") final List<String> names) {
        return memoryService.openNodes(names);
    }
}
