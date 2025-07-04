"""
Simple test runner for config without pytest dependency.
"""

import sys
import tempfile
from pathlib import Path

# Add parent directory to path to import our modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from update_docs.config import DocumentationConfig


def test_from_project_root_with_path_object():
    """Test creating config from Path object."""
    with tempfile.TemporaryDirectory() as temp_dir:
        root_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(root_path)

        assert config.project_root == root_path.resolve()
        assert config.staging_dir == root_path / "staging"
        assert config.readme_path == root_path / "README.md"
        expected_src = root_path / "src" / "main" / "java" / "com" / "jasonriddle" / "mcp"
        assert config.src_dir == expected_src
    print("✓ test_from_project_root_with_path_object")


def test_from_project_root_with_string_path():
    """Test creating config from string path."""
    with tempfile.TemporaryDirectory() as temp_dir:
        config = DocumentationConfig.from_project_root(temp_dir)

        assert config.project_root == Path(temp_dir).resolve()
        assert config.staging_dir == Path(temp_dir) / "staging"
    print("✓ test_from_project_root_with_string_path")


def test_type_mappings_includes_common_java_types():
    """Test that type mappings include common Java types."""
    config = DocumentationConfig.from_project_root(Path.cwd())

    assert config.type_mappings['String'] == 'string'
    assert config.type_mappings['List<String>'] == 'array'
    assert config.type_mappings['Map<String, Object>'] == 'object'
    assert config.type_mappings['boolean'] == 'boolean'
    assert config.type_mappings['int'] == 'number'
    print("✓ test_type_mappings_includes_common_java_types")


def test_tool_categories_includes_expected_categories():
    """Test that tool categories are properly defined."""
    config = DocumentationConfig.from_project_root(Path.cwd())

    expected_categories = [
        'Entity Management',
        'Relationship Management',
        'Observation Management',
        'Graph Operations'
    ]

    for category in expected_categories:
        assert category in config.tool_categories
        assert len(config.tool_categories[category]) > 0
    print("✓ test_tool_categories_includes_expected_categories")


def test_validate_with_missing_directories():
    """Test validation with missing directories."""
    with tempfile.TemporaryDirectory() as temp_dir:
        # Create config pointing to non-existent subdirectories
        root_path = Path(temp_dir) / "nonexistent"
        config = DocumentationConfig.from_project_root(root_path)

        errors = config.validate()

        assert len(errors) > 0
        assert any("Project root does not exist" in error for error in errors)
    print("✓ test_validate_with_missing_directories")


def test_validate_with_existing_structure():
    """Test validation with proper directory structure."""
    with tempfile.TemporaryDirectory() as temp_dir:
        root_path = Path(temp_dir)

        # Create the expected directory structure
        readme_path = root_path / "README.md"
        readme_path.write_text("# Test README")

        src_dir = root_path / "src" / "main" / "java" / "com" / "jasonriddle" / "mcp"
        src_dir.mkdir(parents=True)

        # Create required Java files
        (src_dir / "McpMemoryTools.java").write_text("// Tools file")
        (src_dir / "McpMemoryResources.java").write_text("// Resources file")

        config = DocumentationConfig.from_project_root(root_path)
        errors = config.validate()

        assert len(errors) == 0
    print("✓ test_validate_with_existing_structure")


def test_simplify_java_type_with_known_types():
    """Test simplifying known Java types."""
    config = DocumentationConfig.from_project_root(Path.cwd())

    assert config.simplify_java_type("String") == "string"
    assert config.simplify_java_type("List<String>") == "array"
    assert config.simplify_java_type("Map<String, Object>") == "object"
    assert config.simplify_java_type("boolean") == "boolean"
    print("✓ test_simplify_java_type_with_known_types")


def test_categorize_tool_entity_management():
    """Test categorizing entity management tools."""
    config = DocumentationConfig.from_project_root(Path.cwd())

    assert config.categorize_tool("memory.create_entities") == "Entity Management"
    assert config.categorize_tool("memory.delete_entities") == "Entity Management"
    print("✓ test_categorize_tool_entity_management")


def test_categorize_tool_graph_operations():
    """Test categorizing graph operation tools."""
    config = DocumentationConfig.from_project_root(Path.cwd())

    assert config.categorize_tool("memory.read_graph") == "Graph Operations"
    assert config.categorize_tool("memory.search_nodes") == "Graph Operations"
    assert config.categorize_tool("memory.open_nodes") == "Graph Operations"
    print("✓ test_categorize_tool_graph_operations")


def test_categorize_tool_unknown_tool():
    """Test categorizing unknown tools."""
    config = DocumentationConfig.from_project_root(Path.cwd())

    result = config.categorize_tool("memory.unknown_tool")
    assert result == "Other"
    print("✓ test_categorize_tool_unknown_tool")


def run_all_tests():
    """Run all test functions."""
    test_functions = [
        test_from_project_root_with_path_object,
        test_from_project_root_with_string_path,
        test_type_mappings_includes_common_java_types,
        test_tool_categories_includes_expected_categories,
        test_validate_with_missing_directories,
        test_validate_with_existing_structure,
        test_simplify_java_type_with_known_types,
        test_categorize_tool_entity_management,
        test_categorize_tool_graph_operations,
        test_categorize_tool_unknown_tool,
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
