"""
Simple test runner for regex extractor.
"""

import sys
import tempfile
from pathlib import Path

# Add parent directory to path to import our modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from update_docs.config import DocumentationConfig
from update_docs.extractors.regex_extractor import RegexDataExtractor, RegexPatterns
from update_docs.extractors.base import ExtractionError


def create_test_tools_file(temp_dir: Path) -> Path:
    """Create a test McpMemoryTools.java file."""
    tools_content = '''package com.jasonriddle.mcp;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;

/**
 * Test tools for memory operations.
 */
public final class McpMemoryTools {

    /**
     * Creates entities in the memory graph.
     *
     * @param entities entities to create.
     * @return result message.
     */
    @Tool(name = "memory.create_entities", description = "Create multiple new entities in the knowledge graph")
    public String createEntities(
            @ToolArg(description = "Array of entities with name, entityType, and observations")
                    final List<Map<String, Object>> entities) {
        return "Created entities";
    }

    /**
     * Reads the complete memory graph.
     *
     * @return graph data.
     */
    @Tool(name = "memory.read_graph", description = "Read the entire knowledge graph")
    public List<String> readGraph() {
        return List.of();
    }
}'''

    src_dir = temp_dir / "src" / "main" / "java" / "com" / "jasonriddle" / "mcp"
    src_dir.mkdir(parents=True)

    tools_file = src_dir / "McpMemoryTools.java"
    tools_file.write_text(tools_content)
    return tools_file


def create_test_resources_file(temp_dir: Path) -> Path:
    """Create a test McpMemoryResources.java file."""
    resources_content = '''package com.jasonriddle.mcp;

import io.quarkiverse.mcp.server.Resource;

/**
 * Test resources for memory data.
 */
public final class McpMemoryResources {

    /**
     * Returns the complete memory graph.
     *
     * @return graph resource.
     */
    @Resource(uri = "memory://graph")
    TextResourceContents getGraph() {
        return new TextResourceContents("graph data");
    }

    /**
     * Returns memory status information.
     *
     * @return status resource.
     */
    @Resource(uri = "memory://status")
    TextResourceContents getStatus() {
        return new TextResourceContents("status data");
    }
}'''

    src_dir = temp_dir / "src" / "main" / "java" / "com" / "jasonriddle" / "mcp"
    src_dir.mkdir(parents=True, exist_ok=True)

    resources_file = src_dir / "McpMemoryResources.java"
    resources_file.write_text(resources_content)
    return resources_file


def test_regex_extractor_with_test_files():
    """Test regex extractor with mock Java files."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)

        # Create test files
        create_test_tools_file(temp_path)
        create_test_resources_file(temp_path)

        # Create README for validation
        (temp_path / "README.md").write_text("# Test README")

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = RegexDataExtractor(config)

        # Test tool extraction
        tools = extractor.extract_tools()
        assert len(tools) == 2

        # Check first tool
        create_tool = next((t for t in tools if t['name'] == 'memory.create_entities'), None)
        assert create_tool is not None
        assert create_tool['title'] == 'Create Entities'
        assert create_tool['description'] == 'Create multiple new entities in the knowledge graph'
        assert create_tool['method_name'] == 'createEntities'
        assert create_tool['read_only'] == False
        assert len(create_tool['parameters']) == 1
        assert create_tool['parameters'][0]['name'] == 'entities'

        # Check second tool
        read_tool = next((t for t in tools if t['name'] == 'memory.read_graph'), None)
        assert read_tool is not None
        assert read_tool['title'] == 'Read Graph'
        assert read_tool['read_only'] == True  # List<String> return type

        # Test resource extraction
        resources = extractor.extract_resources()
        assert len(resources) == 2

        # Check first resource
        graph_resource = next((r for r in resources if r['uri'] == 'memory://graph'), None)
        assert graph_resource is not None
        assert graph_resource['title'] == 'Memory Graph'
        assert graph_resource['method_name'] == 'getGraph'

    print("✓ test_regex_extractor_with_test_files")


def test_regex_extractor_with_missing_files():
    """Test regex extractor behavior with missing files."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        extractor = RegexDataExtractor(config)

        # Should raise ExtractionError for missing tools file
        try:
            extractor.extract_tools()
            assert False, "Should have raised ExtractionError"
        except ExtractionError as e:
            assert "Tools file not found" in str(e)

        # Should raise ExtractionError for missing resources file
        try:
            extractor.extract_resources()
            assert False, "Should have raised ExtractionError"
        except ExtractionError as e:
            assert "Resources file not found" in str(e)

    print("✓ test_regex_extractor_with_missing_files")


def test_regex_extractor_extract_all():
    """Test extract_all method."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)

        create_test_tools_file(temp_path)
        create_test_resources_file(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = RegexDataExtractor(config)

        data = extractor.extract_all()

        assert 'tools' in data
        assert 'resources' in data
        assert len(data['tools']) == 2
        assert len(data['resources']) == 2

    print("✓ test_regex_extractor_extract_all")


def test_regex_patterns_find_tool_sections():
    """Test RegexPatterns.find_tool_sections method."""
    content = '''
class TestClass {
    @Tool(name = "tool1", description = "First tool")
    public void method1() {}

    @Tool(name = "tool2", description = "Second tool")
    public void method2() {}

    public void regularMethod() {}
}'''

    sections = RegexPatterns.find_tool_sections(content)
    # Should split into sections starting with @Tool
    tool_sections = [s for s in sections if s.strip().startswith('@Tool')]
    assert len(tool_sections) == 2

    print("✓ test_regex_patterns_find_tool_sections")


def test_regex_patterns_extract_tool_annotation():
    """Test RegexPatterns.extract_tool_annotation method."""
    section = '@Tool(name = "memory.test_tool", description = "A test tool")'

    name, description = RegexPatterns.extract_tool_annotation(section)
    assert name == "memory.test_tool"
    assert description == "A test tool"

    # Test with no annotation
    name, description = RegexPatterns.extract_tool_annotation("no annotation here")
    assert name is None
    assert description is None

    print("✓ test_regex_patterns_extract_tool_annotation")


def test_regex_patterns_extract_method_signature():
    """Test RegexPatterns.extract_method_signature method."""
    section = 'public String testMethod(final String param1, final int param2)'

    return_type, method_name, params = RegexPatterns.extract_method_signature(section)
    assert return_type == "String"
    assert method_name == "testMethod"
    assert "param1" in params
    assert "param2" in params

    print("✓ test_regex_patterns_extract_method_signature")


def test_regex_patterns_find_tool_arg_annotations():
    """Test RegexPatterns.find_tool_arg_annotations method."""
    section = '''
    @ToolArg(description = "First parameter")
    final String param1,
    @ToolArg(description = "Second parameter")
    final List<String> param2)'''

    annotations = RegexPatterns.find_tool_arg_annotations(section)
    assert len(annotations) == 2

    desc1, type1, name1 = annotations[0]
    assert desc1 == "First parameter"
    assert "String" in type1
    assert name1 == "param1"

    desc2, type2, name2 = annotations[1]
    assert desc2 == "Second parameter"
    assert "List<String>" in type2
    assert name2 == "param2"

    print("✓ test_regex_patterns_find_tool_arg_annotations")


def test_regex_extractor_with_actual_project_files():
    """Test regex extractor with the actual project files if they exist."""
    # Use the current project directory
    project_root = Path(__file__).parent.parent.parent
    config = DocumentationConfig.from_project_root(project_root)

    # Only run this test if the actual files exist
    if config.get_tools_file_path().exists() and config.get_resources_file_path().exists():
        extractor = RegexDataExtractor(config)

        # Test that we can extract without errors
        tools = extractor.extract_tools()
        resources = extractor.extract_resources()

        # Basic validation - should find some tools and resources
        assert len(tools) > 0, "Should find at least one tool"
        assert len(resources) > 0, "Should find at least one resource"

        # Check that tool structure is correct
        for tool in tools:
            assert 'name' in tool
            assert 'description' in tool
            assert 'method_name' in tool
            assert 'read_only' in tool
            assert 'parameters' in tool
            assert tool['name'].startswith('memory.')

        # Check that resource structure is correct
        for resource in resources:
            assert 'uri' in resource
            assert 'title' in resource
            assert 'method_name' in resource
            assert resource['uri'].startswith('memory://')

        print("✓ test_regex_extractor_with_actual_project_files")
    else:
        print("⚠ test_regex_extractor_with_actual_project_files (skipped - files not found)")


def run_all_tests():
    """Run all test functions."""
    test_functions = [
        test_regex_extractor_with_test_files,
        test_regex_extractor_with_missing_files,
        test_regex_extractor_extract_all,
        test_regex_patterns_find_tool_sections,
        test_regex_patterns_extract_tool_annotation,
        test_regex_patterns_extract_method_signature,
        test_regex_patterns_find_tool_arg_annotations,
        test_regex_extractor_with_actual_project_files,
    ]

    passed = 0
    failed = 0

    for test_func in test_functions:
        try:
            test_func()
            passed += 1
        except Exception as e:
            print(f"✗ {test_func.__name__}: {e}")
            failed += 1

    print(f"\nTest Results: {passed} passed, {failed} failed")
    return failed == 0


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)
