package com.jasonriddle.mcp;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptMessage;
import io.quarkiverse.mcp.server.TextContent;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * MCP Memory Prompts providing guidance on memory management best practices.
 */
@ApplicationScoped
public final class McpMemoryPrompts {

    /**
     * Provides comprehensive guidance on memory management patterns and best practices.
     *
     * @return memory best practices prompt message
     */
    @Prompt(name = "memory_best_practices", description = "Guide Claude on effective memory management patterns")
    PromptMessage memoryBestPracticesPrompt() {
        String content = buildMemoryGuideContent();
        TextContent textContent = new TextContent(content);
        return PromptMessage.withUserRole(textContent);
    }

    private String buildMemoryGuideContent() {
        return buildHeader()
                + buildEntitySection()
                + buildRelationshipSection()
                + buildSearchSection()
                + buildObservationSection()
                + buildHygieneSection()
                + buildIntegrationSection()
                + buildFooter();
    }

    private String buildHeader() {
        return """
            # Memory Management Best Practices Guide

            This comprehensive guide provides patterns for effective knowledge graph structure, search strategies, and entity management.

            """;
    }

    private String buildEntitySection() {
        return """
            ## Entity Design Principles

            ### Understanding Entities
            Entities are the core nodes in the memory graph. Each entity has:
            - **Name**: Unique identifier (e.g., "Jason", "Technical_Preferences")
            - **Type**: Category classification (e.g., "person", "preferences")
            - **Observations**: List of facts about the entity
            - **Relationships**: Connections to other entities

            ### When to Create New Entities
            - **Distinct, referenceable concepts** that will have relationships with other entities
            - **Things that accumulate observations** over time and become knowledge hubs
            - **Concepts that other entities reference** frequently or serve as connection points

            Examples:
            - ✅ "Jason" (person entity) - will accumulate preferences, relationships, work history
            - ✅ "Technical_Preferences" (preferences entity) - referenced by multiple systems

            ### When to Add Observations Instead
            - **Simple facts** about existing entities that don't need their own relationships
            - **Temporary or frequently changing** information that shouldn't fragment the graph
            - **Detailed attributes** of an existing concept rather than standalone concepts

            Examples:
            - ✅ Add "Prefers dark themes" as observation to existing "Technical_Preferences" entity
            - ✅ Add "Currently working on authentication module" to "Jason" entity

            ### Entity Naming Conventions
            - **Use underscores** for multi-word names: `Technical_Preferences`, `Project_Alpha`
            - **Be descriptive and specific**: `Email_Settings` not just `Settings`
            - **Include context when necessary**: `Personal_Calendar` vs `Work_Calendar`
            - **Stay consistent**: Follow established naming patterns

            ### Entity Type Guidelines
            - **Keep types general but meaningful**: `person`, `preferences`, `project`, `system`
            - **Use hierarchical types sparingly**: Prefer `person` over `employee`, `manager`
            - **Be consistent**: Don't mix `person` and `people`, `preference` and `preferences`

            ## Working with Individual Entities

            ### Finding Entity Details
            Use the `memory.open_nodes` tool to get complete entity information:
            ```
            memory.open_nodes names=["Jason"]
            ```

            ### Exploring Relationships
            - **Outgoing relationships**: What this entity connects to
            - **Incoming relationships**: What connects to this entity
            - Use relationship traversal to explore connected concepts

            ### Entity Modification
            - **Add observations**: Use `memory.add_observations`
            - **Create relationships**: Use `memory.create_relations`
            - **Update information**: Add new observations, archive old ones

            ### Common Entity Patterns
            ```
            # Person entity
            Name: Jason
            Type: person
            Observations: ["Software developer", "Prefers dark themes"]

            # Preferences entity
            Name: Technical_Preferences
            Type: preferences
            Observations: ["Dark mode enabled", "Vim keybindings"]
            ```

            """;
    }

    private String buildRelationshipSection() {
        return """
            ## Relationship Modeling

            ### Active Voice Conventions
            - **Always use active voice**: "Jason has_preferences Technical_Preferences" ✅
            - **Avoid passive constructions**: "Technical_Preferences preferences_of Jason" ❌
            - **Make direction clear**: "Project_Alpha managed_by Jason" ✅

            ### Relationship Type Standards
            - **Be specific and descriptive**: `works_at` not just `related_to`
            - **Include temporal context** when relevant: `currently_uses`, `previously_worked_at`
            - **Use consistent patterns**: `has_preference`, `uses_tool`, `works_on_project`

            ### Common Relationship Patterns
            ```
            # Ownership/Possession
            Jason has_preferences Technical_Preferences
            Jason owns_device Laptop_2023

            # Work/Professional
            Jason works_at Company_Alpha
            Jason manages_project Project_Beta

            # Usage/Interaction
            Jason uses_tool VSCode
            Jason accesses_system Production_Database
            ```

            """;
    }

    private String buildSearchSection() {
        return """
            ## Search and Query Strategies

            ### Available Search Methods

            #### 1. Entity Search by Name
            - Use the `memory.search_nodes` tool with entity names
            - Example: Search for "Jason" to find person entities

            #### 2. Observation Search
            - Search within entity observations for specific content
            - Example: Search for "developer" to find programming-related entities

            #### 3. Entity Type Filtering
            - Use the `memory://types` resource to see available types
            - Search by type like "person", "preferences", "project"

            ### Effective Search Strategies
            - **Use entity types for filtering**: Search within `person` type for people queries
            - **Combine entity and observation searches**: Find people who "use Python"
            - **Leverage relationship traversal**: Find all projects via `works_on_project` relations

            ### Search Best Practices
            - **Use specific terms**: "Python developer" vs "programming"
            - **Try partial matches**: "prefer" matches "preferences", "preferred"
            - **Combine searches**: Search for entities then explore their relationships
            - **Check entity types**: Use type filtering to narrow results

            ### Common Search Patterns
            ```
            # Find all people
            memory.search_nodes query="person"

            # Find technical preferences
            memory.search_nodes query="technical"

            # Find work-related entities
            memory.search_nodes query="work"
            ```

            ### Query Performance Considerations
            - **Favor entity-based searches**: Searching entity names is faster than observation text
            - **Use specific relationship types**: `works_at` search is faster than generic `related_to`
            - **Limit observation text length**: Very long observations slow down text searches

            """;
    }

    private String buildObservationSection() {
        return """
            ## Observation Management

            ### Atomic Observation Principles
            - **One fact per observation**: "Prefers dark themes" not "Prefers dark and monospace"
            - **Be specific and actionable**: "Uses VSCode with Vim extension" not "Likes tools"
            - **Include context when helpful**: "Prefers meetings before 10 AM PST"

            ### Fact vs Opinion Guidelines
            - **Clearly distinguish facts from opinions**: "Uses Python daily" vs "Thinks Python elegant"
            - **Prefer objective observations**: "Commits code every weekday"
            - **Include source or confidence**: "Mentioned preferring TypeScript over JavaScript"

            ### Observation Lifecycle
            - **Update rather than accumulate**: Replace outdated with current preferences
            - **Remove outdated information**: Delete "Learning Python" after "Expert in Python"
            - **Archive rather than delete**: Move old observations to `Previous_Preferences` entity

            """;
    }

    private String buildHygieneSection() {
        return """
            ## Memory Hygiene

            ### Regular Cleanup Patterns
            - **Consolidate duplicate entities**: Merge "Jason" and "Jason_R" into single entity
            - **Remove orphaned relations**: Delete relations where endpoints no longer exist
            - **Merge related observations**: Combine similar observations into comprehensive ones

            ### Duplicate Detection
            - **Similar entity names**: "Technical_Prefs" and "Technical_Preferences"
            - **Redundant observations**: "Likes Python" and "Prefers Python programming"
            - **Equivalent relationships**: "works_at" and "employed_by" pointing to same entities

            ### Graph Validation
            - **Check relationship integrity**: Ensure all relation endpoints exist as entities
            - **Validate observation quality**: Remove empty, duplicate, or meaningless observations
            - **Review entity isolation**: Identify entities with no relationships
            - **Monitor graph growth**: Track entity and relationship counts over time

            """;
    }

    private String buildIntegrationSection() {
        return """
            ## Integration Best Practices

            ### Working with Existing Memory Files
            - **Preserve existing structure**: Don't drastically change established entity names
            - **Extend rather than replace**: Add new observations instead of recreating entities
            - **Maintain consistency**: Follow existing naming and relationship patterns

            ### Cross-Session Consistency
            - **Use standardized entity names**: Establish conventions for common entities
            - **Maintain relationship vocabulary**: Keep consistent relationship type names
            - **Share context entities**: Reference common entities like `Technical_Preferences`

            ## Advanced Patterns

            ### Entity Hierarchies
            - **Use parent-child relationships**: `Project_Alpha contains_module Authentication_Module`
            - **Model inheritance patterns**: `Python_Developer specializes_from Developer`
            - **Group related entities**: `Development_Team includes_member Jason`

            ### Temporal Modeling
            - **Track state changes**: `Jason previously_worked_at Company_A`, `Jason currently_works_at Company_B`
            - **Use time-specific relations**: `Jason used_during_2023 Python`, `Jason learning_since_2024 Rust`
            - **Model evolution**: `Technical_Preferences evolved_from Previous_Preferences`

            """;
    }

    private String buildFooter() {
        return "This guide ensures uniform memory graph structure, effective search patterns, and prevents common modeling mistakes.\n";
    }
}
