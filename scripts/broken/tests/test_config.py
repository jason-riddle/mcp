"""
Tests for configuration management.
"""

import pytest
import tempfile
from pathlib import Path
from update_docs.config import DocumentationConfig


class TestDocumentationConfig:
    """Test cases for DocumentationConfig class."""

    def test_from_project_root_with_path_object(self):
        """Test creating config from Path object."""
        with tempfile.TemporaryDirectory() as temp_dir:
            root_path = Path(temp_dir)
            config = DocumentationConfig.from_project_root(root_path)

            assert config.project_root == root_path.resolve()
            assert config.staging_dir == root_path / "staging"
            assert config.readme_path == root_path / "README.md"
            expected_src = root_path / "src" / "main" / "java" / "com" / "jasonriddle" / "mcp"
            assert config.src_dir == expected_src

    def test_from_project_root_with_string_path(self):
        """Test creating config from string path."""
        with tempfile.TemporaryDirectory() as temp_dir:
            config = DocumentationConfig.from_project_root(temp_dir)

            assert config.project_root == Path(temp_dir).resolve()
            assert config.staging_dir == Path(temp_dir) / "staging"

    def test_type_mappings_includes_common_java_types(self):
        """Test that type mappings include common Java types."""
        config = DocumentationConfig.from_project_root(Path.cwd())

        assert config.type_mappings['String'] == 'string'
        assert config.type_mappings['List<String>'] == 'array'
        assert config.type_mappings['Map<String, Object>'] == 'object'
        assert config.type_mappings['boolean'] == 'boolean'
        assert config.type_mappings['int'] == 'number'

    def test_tool_categories_includes_expected_categories(self):
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

    def test_section_markers_includes_tools_and_resources(self):
        """Test that section markers are defined for tools and resources."""
        config = DocumentationConfig.from_project_root(Path.cwd())

        assert 'tools' in config.section_markers
        assert 'resources' in config.section_markers

        for section in ['tools', 'resources']:
            markers = config.section_markers[section]
            assert 'start_pattern' in markers
            assert 'end_marker' in markers
            assert 'new_start_marker' in markers

    def test_validate_with_missing_directories(self):
        """Test validation with missing directories."""
        with tempfile.TemporaryDirectory() as temp_dir:
            # Create config pointing to non-existent subdirectories
            root_path = Path(temp_dir) / "nonexistent"
            config = DocumentationConfig.from_project_root(root_path)

            errors = config.validate()

            assert len(errors) > 0
            assert any("Project root does not exist" in error for error in errors)

    def test_validate_with_existing_structure(self):
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

    def test_ensure_staging_dir_creates_directory(self):
        """Test that ensure_staging_dir creates the staging directory."""
        with tempfile.TemporaryDirectory() as temp_dir:
            root_path = Path(temp_dir)
            config = DocumentationConfig.from_project_root(root_path)

            assert not config.staging_dir.exists()

            config.ensure_staging_dir()

            assert config.staging_dir.exists()
            assert config.staging_dir.is_dir()

    def test_get_tools_file_path(self):
        """Test getting tools file path."""
        config = DocumentationConfig.from_project_root(Path.cwd())
        tools_path = config.get_tools_file_path()

        expected = config.src_dir / "McpMemoryTools.java"
        assert tools_path == expected

    def test_get_resources_file_path(self):
        """Test getting resources file path."""
        config = DocumentationConfig.from_project_root(Path.cwd())
        resources_path = config.get_resources_file_path()

        expected = config.src_dir / "McpMemoryResources.java"
        assert resources_path == expected

    def test_simplify_java_type_with_known_types(self):
        """Test simplifying known Java types."""
        config = DocumentationConfig.from_project_root(Path.cwd())

        assert config.simplify_java_type("String") == "string"
        assert config.simplify_java_type("List<String>") == "array"
        assert config.simplify_java_type("Map<String, Object>") == "object"
        assert config.simplify_java_type("boolean") == "boolean"

    def test_simplify_java_type_with_unknown_type(self):
        """Test simplifying unknown Java types."""
        config = DocumentationConfig.from_project_root(Path.cwd())

        result = config.simplify_java_type("CustomType")
        assert result == "customtype"

    def test_simplify_java_type_with_whitespace(self):
        """Test simplifying Java types with whitespace."""
        config = DocumentationConfig.from_project_root(Path.cwd())

        result = config.simplify_java_type("  String  ")
        assert result == "string"

    def test_categorize_tool_entity_management(self):
        """Test categorizing entity management tools."""
        config = DocumentationConfig.from_project_root(Path.cwd())

        assert config.categorize_tool("memory.create_entities") == "Entity Management"
        assert config.categorize_tool("memory.delete_entities") == "Entity Management"

    def test_categorize_tool_relationship_management(self):
        """Test categorizing relationship management tools."""
        config = DocumentationConfig.from_project_root(Path.cwd())

        assert config.categorize_tool("memory.create_relations") == "Relationship Management"
        assert config.categorize_tool("memory.delete_relations") == "Relationship Management"

    def test_categorize_tool_observation_management(self):
        """Test categorizing observation management tools."""
        config = DocumentationConfig.from_project_root(Path.cwd())

        assert config.categorize_tool("memory_add_observations") == "Observation Management"
        assert config.categorize_tool("memory_delete_observations") == "Observation Management"

    def test_categorize_tool_graph_operations(self):
        """Test categorizing graph operation tools."""
        config = DocumentationConfig.from_project_root(Path.cwd())

        assert config.categorize_tool("memory.read_graph") == "Graph Operations"
        assert config.categorize_tool("memory.search_nodes") == "Graph Operations"
        assert config.categorize_tool("memory.open_nodes") == "Graph Operations"

    def test_categorize_tool_unknown_tool(self):
        """Test categorizing unknown tools."""
        config = DocumentationConfig.from_project_root(Path.cwd())

        result = config.categorize_tool("memory.unknown_tool")
        assert result == "Other"
