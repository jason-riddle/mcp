package com.jasonriddle.mcp.memory;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Entity represents a node in the knowledge graph.
 *
 * @param type the type of record (always "entity").
 * @param name unique identifier for the entity.
 * @param entityType classification of the entity.
 * @param observations list of atomic facts about the entity.
 */
public record Entity(
        @JsonProperty("type") String type,
        @JsonProperty("name") String name,
        @JsonProperty("entityType") String entityType,
        @JsonProperty("observations") List<String> observations) {

    /**
     * Compact constructor for validation and processing.
     *
     * @param type the type of record.
     * @param name unique identifier for the entity.
     * @param entityType classification of the entity.
     * @param observations list of atomic facts about the entity.
     */
    public Entity {
        if (type == null || type.isEmpty()) {
            type = "entity";
        }
        if (observations == null) {
            observations = List.of();
        } else {
            observations = List.copyOf(observations);
        }
    }

    /**
     * Creates an Entity with the given parameters.
     *
     * @param name unique identifier for the entity.
     * @param entityType classification of the entity.
     * @param observations list of atomic facts about the entity.
     */
    public Entity(final String name, final String entityType, final List<String> observations) {
        this("entity", name, entityType, observations);
    }
}
