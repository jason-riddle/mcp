package com.jasonriddle.mcp.memory;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Relation represents a directed connection between entities.
 *
 * @param type the type of record (always "relation").
 * @param from source entity name.
 * @param to target entity name.
 * @param relationType type of relationship in active voice.
 */
public record Relation(
        @JsonProperty("type") String type,
        @JsonProperty("from") String from,
        @JsonProperty("to") String to,
        @JsonProperty("relationType") String relationType) {

    /**
     * Creates a Relation with the given parameters.
     *
     * @param from source entity name.
     * @param to target entity name.
     * @param relationType type of relationship in active voice.
     */
    public Relation(final String from, final String to, final String relationType) {
        this("relation", from, to, relationType);
    }

    /**
     * Creates a Relation with all parameters.
     *
     * @param type the type of record.
     * @param from source entity name.
     * @param to target entity name.
     * @param relationType type of relationship in active voice.
     */
    public Relation {
        if (type == null || type.isEmpty()) {
            type = "relation";
        }
    }
}
