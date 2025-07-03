package com.jasonriddle.mcp.memory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * MemoryRecord is a marker interface for JSONL records.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        defaultImpl = Entity.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Entity.class, name = "entity"),
    @JsonSubTypes.Type(value = Relation.class, name = "relation")
})
public interface MemoryRecord {
    /**
     * Gets the type of this record.
     *
     * @return the record type
     */
    @JsonProperty("type")
    String type();
}
