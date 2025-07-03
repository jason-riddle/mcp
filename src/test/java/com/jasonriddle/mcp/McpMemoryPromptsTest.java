package com.jasonriddle.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkiverse.mcp.server.PromptMessage;
import io.quarkiverse.mcp.server.TextContent;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for McpMemoryPrompts.
 */
final class McpMemoryPromptsTest {

    private final McpMemoryPrompts mcpMemoryPrompts = new McpMemoryPrompts();

    @Test
    void memoryBestPracticesPromptShouldReturnComprehensiveGuide() {
        PromptMessage result = mcpMemoryPrompts.memoryBestPracticesPrompt();

        assertNotNull(result);
        assertEquals("USER", result.role().toString());

        TextContent content = (TextContent) result.content();
        String text = content.text();

        // Check main sections are present
        assertTrue(text.contains("Memory Management Best Practices Guide"));
        assertTrue(text.contains("Entity Design Principles"));
        assertTrue(text.contains("Relationship Modeling"));
        assertTrue(text.contains("Observation Management"));
        assertTrue(text.contains("Search and Query Strategies"));
        assertTrue(text.contains("Memory Hygiene"));

        // Check specific guidelines
        assertTrue(text.contains("When to Create New Entities"));
        assertTrue(text.contains("When to Add Observations Instead"));
        assertTrue(text.contains("Active Voice Conventions"));
        assertTrue(text.contains("Atomic Observation Principles"));
        assertTrue(text.contains("Effective Search Strategies"));
        assertTrue(text.contains("Regular Cleanup Patterns"));

        // Check practical examples are included
        assertTrue(text.contains("Jason has_preferences Technical_Preferences"));
        assertTrue(text.contains("Use underscores"));
        assertTrue(text.contains("One fact per observation"));

        // Check advanced patterns
        assertTrue(text.contains("Advanced Patterns"));
        assertTrue(text.contains("Entity Hierarchies"));
        assertTrue(text.contains("Temporal Modeling"));
        assertTrue(text.contains("Integration Best Practices"));
    }

    @Test
    void memoryBestPracticesPromptShouldIncludeNamingConventions() {
        PromptMessage result = mcpMemoryPrompts.memoryBestPracticesPrompt();
        TextContent content = (TextContent) result.content();
        String text = content.text();

        assertTrue(text.contains("Entity Naming Conventions"));
        assertTrue(text.contains("Use underscores"));
        assertTrue(text.contains("Technical_Preferences"));
        assertTrue(text.contains("Be descriptive and specific"));
        assertTrue(text.contains("Email_Settings"));
    }

    @Test
    void memoryBestPracticesPromptShouldIncludeRelationshipGuidelines() {
        PromptMessage result = mcpMemoryPrompts.memoryBestPracticesPrompt();
        TextContent content = (TextContent) result.content();
        String text = content.text();

        assertTrue(text.contains("Active Voice Conventions"));
        assertTrue(text.contains("Always use active voice"));
        assertTrue(text.contains("works_at"));
        assertTrue(text.contains("currently_uses"));
        assertTrue(text.contains("previously_worked_at"));
    }

    @Test
    void memoryBestPracticesPromptShouldIncludeDataIntegrityGuidance() {
        PromptMessage result = mcpMemoryPrompts.memoryBestPracticesPrompt();
        TextContent content = (TextContent) result.content();
        String text = content.text();

        assertTrue(text.contains("Graph Validation"));
        assertTrue(text.contains("Check relationship integrity"));
        assertTrue(text.contains("Validate observation quality"));
        assertTrue(text.contains("Review entity isolation"));
        assertTrue(text.contains("Monitor graph growth"));
    }

    @Test
    void memoryBestPracticesPromptShouldIncludeIntegrationBestPractices() {
        PromptMessage result = mcpMemoryPrompts.memoryBestPracticesPrompt();
        TextContent content = (TextContent) result.content();
        String text = content.text();

        assertTrue(text.contains("Working with Existing Memory Files"));
        assertTrue(text.contains("Cross-Session Consistency"));
        assertTrue(text.contains("Preserve existing structure"));
        assertTrue(text.contains("Use standardized entity names"));
        assertTrue(text.contains("Maintain relationship vocabulary"));
    }
}
