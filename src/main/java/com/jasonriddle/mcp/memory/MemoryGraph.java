package com.jasonriddle.mcp.memory;

import java.util.List;

/**
 * MemoryGraph represents the complete knowledge graph structure.
 *
 * @param entities list of all entities in the graph.
 * @param relations list of all relations in the graph.
 */
public record MemoryGraph(List<Entity> entities, List<Relation> relations) {

    /**
     * Creates a MemoryGraph with the given parameters.
     *
     * @param entities list of all entities in the graph.
     * @param relations list of all relations in the graph.
     */
    public MemoryGraph {
        if (entities != null) {
            entities = List.copyOf(entities);
        } else {
            entities = List.of();
        }
        if (relations != null) {
            relations = List.copyOf(relations);
        } else {
            relations = List.of();
        }
    }

    /**
     * Creates an empty MemoryGraph.
     *
     * @return empty memory graph.
     */
    public static MemoryGraph empty() {
        return new MemoryGraph(List.of(), List.of());
    }
}
