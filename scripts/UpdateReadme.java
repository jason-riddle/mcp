/*
 * Copyright (c) 2025 Jason Riddle.
 *
 * Licensed under the MIT License.
 */

import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Java-based README documentation generator for MCP tools and resources.
 * Equivalent to the Python update-readme.py script but using Java reflection.
 */
public final class UpdateReadme {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    private static final Path TARGET_CLASSES = PROJECT_ROOT.resolve("target").resolve("classes");
    private static final Path README_PATH = PROJECT_ROOT.resolve("README.md");

    private UpdateReadme() {
        // Utility class
    }

    /**
     * Main entry point for the documentation generator.
     *
     * @param args command line arguments (unused).
     */
    public static void main(final String[] args) {
        try {
            System.out.println("Parsing MCP tools and resources from compiled classes...");

            final UpdateReadme generator = new UpdateReadme();
            final boolean success = generator.updateReadme();

            System.exit(success ? 0 : 1);
        } catch (final Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Updates README.md with generated tool and resource documentation.
     *
     * @return true if successful, false otherwise.
     * @throws Exception if an error occurs during processing.
     */
    public boolean updateReadme() throws Exception {
        if (!Files.exists(TARGET_CLASSES)) {
            System.err.println("ERROR: Compiled classes not found. Run 'make build' first.");
            return false;
        }

        if (!Files.exists(README_PATH)) {
            System.err.println("ERROR: README.md not found at " + README_PATH);
            return false;
        }

        // Load classes and extract tools/resources
        final List<ToolInfo> tools = extractTools();
        final List<ResourceInfo> resources = extractResources();

        System.out.println("Found " + tools.size() + " tools and " + resources.size() + " resources");

        // Update README.md
        String content = Files.readString(README_PATH);
        content = updateToolsSection(content, tools);
        content = updateResourcesSection(content, resources);

        Files.writeString(README_PATH, content);
        System.out.println("README.md updated successfully");
        return true;
    }

    /**
     * Extracts MCP tool information from compiled classes.
     *
     * @return list of tool information.
     * @throws Exception if an error occurs during extraction.
     */
    private List<ToolInfo> extractTools() throws Exception {
        final List<ToolInfo> tools = new ArrayList<>();

        try (final URLClassLoader classLoader = new URLClassLoader(new URL[]{TARGET_CLASSES.toUri().toURL()})) {
            final Class<?> toolsClass = classLoader.loadClass("com.jasonriddle.mcp.McpMemoryTools");

            for (final Method method : toolsClass.getDeclaredMethods()) {
                final Tool toolAnnotation = method.getAnnotation(Tool.class);
                if (toolAnnotation != null) {
                    final ToolInfo tool = new ToolInfo();
                    tool.name = toolAnnotation.name();
                    tool.title = formatTitle(toolAnnotation.name());
                    tool.description = toolAnnotation.description();
                    tool.methodName = method.getName();
                    tool.returnType = method.getReturnType().getSimpleName();
                    tool.parameters = extractParameters(method);
                    tool.readOnly = isReadOnlyTool(tool.name, tool.returnType);
                    tools.add(tool);
                }
            }
        }

        return tools;
    }

    /**
     * Extracts MCP resource information from compiled classes.
     *
     * @return list of resource information.
     * @throws Exception if an error occurs during extraction.
     */
    private List<ResourceInfo> extractResources() throws Exception {
        final List<ResourceInfo> resources = new ArrayList<>();

        try (final URLClassLoader classLoader = new URLClassLoader(new URL[]{TARGET_CLASSES.toUri().toURL()})) {
            final Class<?> resourcesClass = classLoader.loadClass("com.jasonriddle.mcp.McpMemoryResources");

            for (final Method method : resourcesClass.getDeclaredMethods()) {
                final Resource resourceAnnotation = method.getAnnotation(Resource.class);
                if (resourceAnnotation != null) {
                    final ResourceInfo resource = new ResourceInfo();
                    resource.uri = resourceAnnotation.uri();
                    resource.methodName = method.getName();
                    resource.title = formatResourceTitle(resource.uri);
                    resource.description = extractJavadocDescription(resourcesClass, method.getName());
                    resources.add(resource);
                }
            }
        }

        return resources;
    }

    /**
     * Extracts parameter information from a method.
     *
     * @param method the method to extract parameters from.
     * @return list of parameter information.
     */
    private List<ParameterInfo> extractParameters(final Method method) {
        final List<ParameterInfo> parameters = new ArrayList<>();

        for (final Parameter parameter : method.getParameters()) {
            final ToolArg toolArgAnnotation = parameter.getAnnotation(ToolArg.class);
            if (toolArgAnnotation != null) {
                final ParameterInfo paramInfo = new ParameterInfo();
                paramInfo.name = parameter.getName();
                paramInfo.type = simplifyType(parameter.getParameterizedType());
                paramInfo.description = toolArgAnnotation.description();
                paramInfo.required = true; // All parameters appear to be required in this codebase
                parameters.add(paramInfo);
            }
        }

        return parameters;
    }

    /**
     * Simplifies Java types to more readable documentation types.
     *
     * @param type the Java type to simplify.
     * @return simplified type string.
     */
    private String simplifyType(final Type type) {
        final Map<String, String> typeMapping = Map.of(
            "String", "string",
            "List<String>", "array",
            "List<Map<String, Object>>", "array",
            "List<Map<String, String>>", "array",
            "Map<String, Object>", "object",
            "Map<String, String>", "object",
            "boolean", "boolean",
            "int", "number",
            "long", "number"
        );

        String typeName = type.getTypeName();

        // Handle parameterized types
        if (type instanceof ParameterizedType) {
            final ParameterizedType paramType = (ParameterizedType) type;
            final Type rawType = paramType.getRawType();
            final Type[] typeArgs = paramType.getActualTypeArguments();

            if (rawType.getTypeName().equals("java.util.List") && typeArgs.length == 1) {
                return "array";
            } else if (rawType.getTypeName().equals("java.util.Map") && typeArgs.length == 2) {
                return "object";
            }
        }

        // Remove package names
        final String simpleName = typeName.substring(typeName.lastIndexOf('.') + 1);
        return typeMapping.getOrDefault(simpleName, simpleName.toLowerCase());
    }

    /**
     * Determines if a tool is read-only based on its name and return type.
     *
     * @param toolName the tool name.
     * @param returnType the return type.
     * @return true if the tool is read-only.
     */
    private boolean isReadOnlyTool(final String toolName, final String returnType) {
        return !"String".equals(returnType) || toolName.toLowerCase().contains("read")
            || toolName.toLowerCase().contains("search") || toolName.toLowerCase().contains("open");
    }

    /**
     * Extracts javadoc description from source files.
     *
     * @param clazz the class containing the method.
     * @param methodName the method name.
     * @return the javadoc description or empty string.
     */
    private String extractJavadocDescription(final Class<?> clazz, final String methodName) {
        try {
            final Path sourceFile = PROJECT_ROOT.resolve("src").resolve("main").resolve("java")
                .resolve("com").resolve("jasonriddle").resolve("mcp")
                .resolve(clazz.getSimpleName() + ".java");

            if (!Files.exists(sourceFile)) {
                return "";
            }

            final String content = Files.readString(sourceFile);
            final Pattern methodPattern = Pattern.compile(
                "/\\*\\*\\s*\\n(.*?)\\*/\\s*@Resource.*?" + methodName + "\\s*\\(",
                Pattern.DOTALL
            );

            final Matcher matcher = methodPattern.matcher(content);
            if (matcher.find()) {
                final String javadoc = matcher.group(1);
                return javadoc.lines()
                    .map(line -> line.replaceFirst("^\\s*\\*\\s?", ""))
                    .filter(line -> !line.trim().isEmpty())
                    .filter(line -> !line.trim().startsWith("@"))
                    .collect(Collectors.joining(" "))
                    .trim();
            }
        } catch (final IOException e) {
            System.err.println("Warning: Could not read source file for javadoc extraction: " + e.getMessage());
        }

        return "";
    }

    /**
     * Formats a tool name into a readable title.
     *
     * @param toolName the tool name.
     * @return formatted title.
     */
    private String formatTitle(final String toolName) {
        final String[] parts = toolName.replace("memory.", "").split("_");
        return Arrays.stream(parts)
            .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
            .collect(Collectors.joining(" "));
    }

    /**
     * Formats a resource URI into a readable title.
     *
     * @param uri the resource URI.
     * @return formatted title.
     */
    private String formatResourceTitle(final String uri) {
        final String resourceName = uri.replace("memory://", "");
        return "Memory " + resourceName.substring(0, 1).toUpperCase() + resourceName.substring(1);
    }

    /**
     * Updates the tools section in README content.
     *
     * @param content the README content.
     * @param tools the list of tools.
     * @return updated content.
     */
    private String updateToolsSection(final String content, final List<ToolInfo> tools) {
        final String toolsContent = generateToolsContent(tools);
        return updateSection(content, "tools", toolsContent);
    }

    /**
     * Updates the resources section in README content.
     *
     * @param content the README content.
     * @param resources the list of resources.
     * @return updated content.
     */
    private String updateResourcesSection(final String content, final List<ResourceInfo> resources) {
        final String resourcesContent = generateResourcesContent(resources);
        return updateSection(content, "resources", resourcesContent);
    }

    /**
     * Updates a section between markers in the content.
     *
     * @param content the content to update.
     * @param sectionType the section type (tools or resources).
     * @param newContent the new content.
     * @return updated content.
     */
    private String updateSection(final String content, final String sectionType, final String newContent) {
        // Look for flexible start marker pattern
        final String startPattern = "<!--- " + sectionType.substring(0, 1).toUpperCase() +
                                   sectionType.substring(1) + " generated by .* -->";
        final String endMarker = "<!--- End of " + sectionType + " generated section -->";

        // Find the start marker using regex
        final Pattern pattern = Pattern.compile(startPattern);
        final Matcher matcher = pattern.matcher(content);

        if (!matcher.find()) {
            System.err.println("WARNING: Start marker pattern '" + startPattern + "' not found in README");
            return content;
        }

        final int startIdx = matcher.end();
        final int endIdx = content.indexOf(endMarker);

        if (endIdx == -1) {
            System.err.println("WARNING: End marker '" + endMarker + "' not found in README");
            return content;
        }

        // Generate new start marker
        final String newStartMarker = "<!--- " + sectionType.substring(0, 1).toUpperCase() +
                                     sectionType.substring(1) + " generated by UpdateReadme.java -->";

        return content.substring(0, matcher.start()) +
               newStartMarker + "\n\n" + newContent + "\n\n" +
               content.substring(endIdx);
    }

    /**
     * Generates formatted content for the tools section.
     *
     * @param tools the list of tools.
     * @return formatted tools content.
     */
    private String generateToolsContent(final List<ToolInfo> tools) {
        if (tools.isEmpty()) {
            return "No tools found.";
        }

        final StringBuilder content = new StringBuilder();

        // Group tools by category
        final Map<String, List<ToolInfo>> categories = new LinkedHashMap<>();
        categories.put("Entity Management", tools.stream()
            .filter(t -> t.name.contains("create_entities") || t.name.contains("delete_entities"))
            .collect(Collectors.toList()));
        categories.put("Relationship Management", tools.stream()
            .filter(t -> t.name.contains("create_relations") || t.name.contains("delete_relations"))
            .collect(Collectors.toList()));
        categories.put("Observation Management", tools.stream()
            .filter(t -> t.name.contains("add_observations") || t.name.contains("delete_observations"))
            .collect(Collectors.toList()));
        categories.put("Graph Operations", tools.stream()
            .filter(t -> t.name.contains("read_graph") || t.name.contains("search_nodes") || t.name.contains("open_nodes"))
            .collect(Collectors.toList()));

        for (final Map.Entry<String, List<ToolInfo>> entry : categories.entrySet()) {
            final List<ToolInfo> categoryTools = entry.getValue();
            if (categoryTools.isEmpty()) {
                continue;
            }

            content.append("<details>\n");
            content.append("<summary><b>").append(entry.getKey()).append("</b></summary>\n\n");

            for (final ToolInfo tool : categoryTools) {
                content.append(formatTool(tool));
            }

            content.append("</details>\n\n");
        }

        return content.toString();
    }

    /**
     * Generates formatted content for the resources section.
     *
     * @param resources the list of resources.
     * @return formatted resources content.
     */
    private String generateResourcesContent(final List<ResourceInfo> resources) {
        if (resources.isEmpty()) {
            return "No resources found.";
        }

        final StringBuilder content = new StringBuilder();
        content.append("<details>\n<summary><b>Memory Resources</b></summary>\n\n");

        for (final ResourceInfo resource : resources) {
            content.append(formatResource(resource));
        }

        content.append("</details>\n\n");
        return content.toString();
    }

    /**
     * Formats a single tool for documentation.
     *
     * @param tool the tool to format.
     * @return formatted tool documentation.
     */
    private String formatTool(final ToolInfo tool) {
        final StringBuilder content = new StringBuilder();
        content.append("<!-- NOTE: This has been generated via UpdateReadme.java -->\n\n");
        content.append("- **").append(tool.name).append("**\n");
        content.append("  - Title: ").append(tool.title).append("\n");
        content.append("  - Description: ").append(tool.description).append("\n");

        if (!tool.parameters.isEmpty()) {
            content.append("  - Parameters:\n");
            for (final ParameterInfo param : tool.parameters) {
                final String requiredText = param.required ? "" : ", optional";
                content.append("    - `").append(param.name).append("` (")
                       .append(param.type).append(requiredText).append("): ")
                       .append(param.description).append("\n");
            }
        } else {
            content.append("  - Parameters: None\n");
        }

        content.append("  - Read-only: **").append(tool.readOnly).append("**\n\n");
        return content.toString();
    }

    /**
     * Formats a single resource for documentation.
     *
     * @param resource the resource to format.
     * @return formatted resource documentation.
     */
    private String formatResource(final ResourceInfo resource) {
        final StringBuilder content = new StringBuilder();
        content.append("<!-- NOTE: This has been generated via UpdateReadme.java -->\n\n");
        content.append("- **").append(resource.uri).append("**\n");
        content.append("  - Title: ").append(resource.title).append("\n");
        content.append("  - Description: ").append(resource.description).append("\n\n");
        return content.toString();
    }

    /**
     * Data class for tool information.
     */
    private static final class ToolInfo {
        String name;
        String title;
        String description;
        String methodName;
        String returnType;
        List<ParameterInfo> parameters = new ArrayList<>();
        boolean readOnly;
    }

    /**
     * Data class for parameter information.
     */
    private static final class ParameterInfo {
        String name;
        String type;
        String description;
        boolean required;
    }

    /**
     * Data class for resource information.
     */
    private static final class ResourceInfo {
        String uri;
        String methodName;
        String title;
        String description;
    }
}
