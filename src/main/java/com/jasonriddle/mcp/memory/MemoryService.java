package com.jasonriddle.mcp.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Service for managing memory graph persistence and operations.
 */
@ApplicationScoped
public class MemoryService {
    private static final Logger LOG = Logger.getLogger(MemoryService.class);

    private final ObjectMapper objectMapper;
    private final Path memoryFilePath;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates a new MemoryService.
     *
     * @param objectMapper json object mapper.
     * @param memoryFilePath path to memory file from config.
     */
    @Inject
    public MemoryService(
            final ObjectMapper objectMapper,
            @ConfigProperty(name = "memory.file.path", defaultValue = "memory.jsonl") final String memoryFilePath) {
        this.objectMapper = objectMapper;
        this.memoryFilePath = Paths.get(memoryFilePath);
        initializeMemoryFile();
    }

    /**
     * Initializes the memory file if it doesn't exist.
     */
    private void initializeMemoryFile() {
        try {
            if (!Files.exists(memoryFilePath)) {
                Files.createFile(memoryFilePath);
                LOG.info("Created memory file at: " + memoryFilePath);
            }
        } catch (IOException e) {
            LOG.error("Failed to initialize memory file", e);
        }
    }

    /**
     * Reads the entire memory graph from the JSONL file.
     *
     * @return the complete memory graph.
     */
    public MemoryGraph readGraph() {
        lock.readLock().lock();
        try {
            List<Entity> entities = new ArrayList<>();
            List<Relation> relations = new ArrayList<>();

            if (!Files.exists(memoryFilePath)) {
                return MemoryGraph.empty();
            }

            try (BufferedReader reader = Files.newBufferedReader(memoryFilePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    Map<String, Object> record = objectMapper.readValue(line, Map.class);
                    String type = (String) record.get("type");

                    if ("entity".equals(type)) {
                        Entity entity = objectMapper.convertValue(record, Entity.class);
                        entities.add(entity);
                    } else if ("relation".equals(type)) {
                        Relation relation = objectMapper.convertValue(record, Relation.class);
                        relations.add(relation);
                    }
                }
            }

            return new MemoryGraph(entities, relations);
        } catch (IOException e) {
            LOG.error("Failed to read memory graph", e);
            return MemoryGraph.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Writes the entire memory graph to the JSONL file.
     *
     * @param graph the memory graph to write.
     */
    private void writeGraph(final MemoryGraph graph) {
        lock.writeLock().lock();
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    memoryFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                // Write entities
                for (Entity entity : graph.entities()) {
                    writer.write(objectMapper.writeValueAsString(entity));
                    writer.newLine();
                }

                // Write relations
                for (Relation relation : graph.relations()) {
                    writer.write(objectMapper.writeValueAsString(relation));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to write memory graph", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates multiple new entities in the graph.
     *
     * @param entities entities to create.
     * @return created entities.
     */
    public List<Entity> createEntities(final List<Entity> entities) {
        lock.writeLock().lock();
        try {
            MemoryGraph graph = readGraph();
            Map<String, Entity> entityMap = new HashMap<>();

            // Build map of existing entities
            for (Entity entity : graph.entities()) {
                entityMap.put(entity.name(), entity);
            }

            // Add new entities (skip duplicates)
            List<Entity> created = new ArrayList<>();
            for (Entity newEntity : entities) {
                if (!entityMap.containsKey(newEntity.name())) {
                    entityMap.put(newEntity.name(), newEntity);
                    created.add(newEntity);
                }
            }

            // Write updated graph
            MemoryGraph updatedGraph = new MemoryGraph(new ArrayList<>(entityMap.values()), graph.relations());
            writeGraph(updatedGraph);

            return created;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates multiple new relations in the graph.
     *
     * @param relations relations to create.
     * @return created relations.
     */
    public List<Relation> createRelations(final List<Relation> relations) {
        lock.writeLock().lock();
        try {
            MemoryGraph graph = readGraph();
            List<Relation> existingRelations = new ArrayList<>(graph.relations());
            List<Relation> created = new ArrayList<>();

            for (Relation newRelation : relations) {
                boolean exists = false;
                for (Relation r : existingRelations) {
                    final boolean fromMatches = r.from().equals(newRelation.from());
                    final boolean toMatches = r.to().equals(newRelation.to());
                    final boolean typeMatches = r.relationType().equals(newRelation.relationType());
                    final boolean allFieldsMatch = fromMatches && toMatches;
                    if (allFieldsMatch && typeMatches) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    existingRelations.add(newRelation);
                    created.add(newRelation);
                }
            }

            MemoryGraph updatedGraph = new MemoryGraph(graph.entities(), existingRelations);
            writeGraph(updatedGraph);

            return created;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds observations to existing entities.
     *
     * @param observationMap map of entity names to new observations.
     * @return map of entity names to added observations.
     */
    public Map<String, List<String>> addObservations(final Map<String, List<String>> observationMap) {
        lock.writeLock().lock();
        try {
            MemoryGraph graph = readGraph();
            Map<String, Entity> entityMap = new HashMap<>();
            Map<String, List<String>> added = new HashMap<>();

            // Build map of existing entities
            for (Entity entity : graph.entities()) {
                entityMap.put(entity.name(), entity);
            }

            // Add observations to existing entities
            for (Map.Entry<String, List<String>> entry : observationMap.entrySet()) {
                String entityName = entry.getKey();
                List<String> newObservations = entry.getValue();

                Entity entity = entityMap.get(entityName);
                if (entity != null) {
                    List<String> updatedObservations = new ArrayList<>(entity.observations());
                    updatedObservations.addAll(newObservations);

                    Entity updatedEntity = new Entity(entity.name(), entity.entityType(), updatedObservations);

                    entityMap.put(entityName, updatedEntity);
                    added.put(entityName, newObservations);
                }
            }

            MemoryGraph updatedGraph = new MemoryGraph(new ArrayList<>(entityMap.values()), graph.relations());
            writeGraph(updatedGraph);

            return added;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes entities and their relations.
     *
     * @param entityNames names of entities to delete.
     * @return deleted entity names.
     */
    public List<String> deleteEntities(final List<String> entityNames) {
        lock.writeLock().lock();
        try {
            MemoryGraph graph = readGraph();

            // Filter out entities to delete
            List<Entity> remainingEntities = new ArrayList<>();
            for (Entity e : graph.entities()) {
                boolean shouldDelete = entityNames.contains(e.name());
                if (!shouldDelete) {
                    remainingEntities.add(e);
                }
            }

            // Filter out relations involving deleted entities
            List<Relation> remainingRelations = new ArrayList<>();
            for (Relation r : graph.relations()) {
                boolean fromDeleted = entityNames.contains(r.from());
                boolean toDeleted = entityNames.contains(r.to());
                if (!fromDeleted && !toDeleted) {
                    remainingRelations.add(r);
                }
            }

            MemoryGraph updatedGraph = new MemoryGraph(remainingEntities, remainingRelations);
            writeGraph(updatedGraph);

            return entityNames;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes specific observations from entities.
     *
     * @param deletionMap map of entity names to observations to delete.
     * @return map of entity names to deleted observations.
     */
    public Map<String, List<String>> deleteObservations(final Map<String, List<String>> deletionMap) {
        lock.writeLock().lock();
        try {
            MemoryGraph graph = readGraph();
            Map<String, Entity> entityMap = new HashMap<>();
            Map<String, List<String>> deleted = new HashMap<>();

            // Build map of existing entities
            for (Entity entity : graph.entities()) {
                entityMap.put(entity.name(), entity);
            }

            // Delete observations from entities
            for (Map.Entry<String, List<String>> entry : deletionMap.entrySet()) {
                String entityName = entry.getKey();
                List<String> observationsToDelete = entry.getValue();

                Entity entity = entityMap.get(entityName);
                if (entity != null) {
                    List<String> remainingObservations = new ArrayList<>(entity.observations());
                    List<String> actuallyDeleted = new ArrayList<>();

                    for (String obs : observationsToDelete) {
                        if (remainingObservations.remove(obs)) {
                            actuallyDeleted.add(obs);
                        }
                    }

                    if (!actuallyDeleted.isEmpty()) {
                        Entity updatedEntity = new Entity(entity.name(), entity.entityType(), remainingObservations);

                        entityMap.put(entityName, updatedEntity);
                        deleted.put(entityName, actuallyDeleted);
                    }
                }
            }

            MemoryGraph updatedGraph = new MemoryGraph(new ArrayList<>(entityMap.values()), graph.relations());
            writeGraph(updatedGraph);

            return deleted;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes specific relations.
     *
     * @param relations relations to delete.
     * @return deleted relations.
     */
    public List<Relation> deleteRelations(final List<Relation> relations) {
        lock.writeLock().lock();
        try {
            MemoryGraph graph = readGraph();
            List<Relation> remainingRelations = new ArrayList<>(graph.relations());
            List<Relation> deleted = new ArrayList<>();

            for (Relation toDelete : relations) {
                final List<Relation> toRemove = new ArrayList<>();
                for (Relation r : remainingRelations) {
                    final boolean fromMatches = r.from().equals(toDelete.from());
                    final boolean toMatches = r.to().equals(toDelete.to());
                    final boolean typeMatches = r.relationType().equals(toDelete.relationType());
                    final boolean allMatch = fromMatches && toMatches;
                    if (allMatch && typeMatches) {
                        toRemove.add(r);
                    }
                }
                remainingRelations.removeAll(toRemove);
                deleted.add(toDelete);
            }

            MemoryGraph updatedGraph = new MemoryGraph(graph.entities(), remainingRelations);
            writeGraph(updatedGraph);

            return deleted;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Searches for nodes based on query.
     *
     * @param query search query.
     * @return matching entities and their relations.
     */
    public MemoryGraph searchNodes(final String query) {
        lock.readLock().lock();
        try {
            MemoryGraph graph = readGraph();
            String lowerQuery = query.toLowerCase();

            List<Entity> matchingEntities = findMatchingEntities(graph, lowerQuery);
            List<Relation> matchingRelations = findMatchingRelations(graph, matchingEntities);

            return new MemoryGraph(matchingEntities, matchingRelations);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<Entity> findMatchingEntities(final MemoryGraph graph, final String lowerQuery) {
        List<Entity> matchingEntities = new ArrayList<>();
        for (Entity e : graph.entities()) {
            if (entityMatches(e, lowerQuery)) {
                matchingEntities.add(e);
            }
        }
        return matchingEntities;
    }

    private boolean entityMatches(final Entity entity, final String lowerQuery) {
        if (entity.name().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        if (entity.entityType().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        return hasMatchingObservation(entity, lowerQuery);
    }

    private boolean hasMatchingObservation(final Entity entity, final String lowerQuery) {
        for (String observation : entity.observations()) {
            if (observation.toLowerCase().contains(lowerQuery)) {
                return true;
            }
        }
        return false;
    }

    private List<Relation> findMatchingRelations(final MemoryGraph graph, final List<Entity> matchingEntities) {
        List<String> entityNames = new ArrayList<>();
        for (Entity entity : matchingEntities) {
            entityNames.add(entity.name());
        }

        List<Relation> matchingRelations = new ArrayList<>();
        for (Relation r : graph.relations()) {
            if (entityNames.contains(r.from()) && entityNames.contains(r.to())) {
                matchingRelations.add(r);
            }
        }
        return matchingRelations;
    }

    /**
     * Opens specific nodes by name.
     *
     * @param names entity names to retrieve.
     * @return entities and their relations.
     */
    public MemoryGraph openNodes(final List<String> names) {
        lock.readLock().lock();
        try {
            MemoryGraph graph = readGraph();

            // Find requested entities
            List<Entity> requestedEntities = new ArrayList<>();
            for (Entity e : graph.entities()) {
                if (names.contains(e.name())) {
                    requestedEntities.add(e);
                }
            }

            // Find relations between requested entities
            List<Relation> relevantRelations = new ArrayList<>();
            for (Relation r : graph.relations()) {
                boolean fromRequested = names.contains(r.from());
                boolean toRequested = names.contains(r.to());
                if (fromRequested && toRequested) {
                    relevantRelations.add(r);
                }
            }

            return new MemoryGraph(requestedEntities, relevantRelations);
        } finally {
            lock.readLock().unlock();
        }
    }
}
