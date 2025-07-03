"""
Simple test runner for generators.
"""

import sys
import tempfile
from pathlib import Path

# Add parent directory to path to import our modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from update_docs.config import DocumentationConfig
from update_docs.generators.tools_generator import (
    ToolsGenerator, ToolsFormatter, ToolsSorter, ToolsValidator
)
from update_docs.generators.resources_generator import (
    ResourcesGenerator, ResourcesFormatter, ResourcesSorter, ResourcesValidator, ResourcesEnhancer
)


def create_sample_tools_data():
    """Create sample tools data for testing."""
    return [
        {
            'name': 'memory.create_entities',
            'title': 'Create Entities',
            'description': 'Create multiple new entities in the knowledge graph',
            'method_name': 'createEntities',
            'return_type': 'String',
            'parameters': [
                {
                    'name': 'entities',
                    'type': 'array',
                    'description': 'Array of entities with name, entityType, and observations',
                    'required': True
                }
            ],
            'read_only': False
        },
        {
            'name': 'memory.read_graph',
            'title': 'Read Graph',
            'description': 'Read the entire knowledge graph',
            'method_name': 'readGraph',
            'return_type': 'List<String>',
            'parameters': [],
            'read_only': True
        }
    ]


def create_sample_resources_data():
    """Create sample resources data for testing."""
    return [
        {
            'uri': 'memory://graph',
            'title': 'Memory Graph',
            'description': 'Returns the complete knowledge graph in formatted way',
            'method_name': 'getGraph'
        },
        {
            'uri': 'memory://status',
            'title': 'Memory Status',
            'description': 'Returns memory graph status information',
            'method_name': 'getStatus'
        }
    ]


def test_tools_generator_with_sample_data():
    """Test tools generator with sample data."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = ToolsGenerator(config)

        tools = create_sample_tools_data()
        markdown = generator.generate_section(tools)

        # Check that markdown contains expected sections
        assert "<details>" in markdown
        assert "Entity Management" in markdown
        assert "Graph Operations" in markdown
        assert "memory.create_entities" in markdown
        assert "memory.read_graph" in markdown
        assert "Create Entities" in markdown
        assert "Read Graph" in markdown
        assert "Read-only: **false**" in markdown
        assert "Read-only: **true**" in markdown

    print("✓ test_tools_generator_with_sample_data")


def test_tools_generator_with_empty_data():
    """Test tools generator with empty data."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = ToolsGenerator(config)

        markdown = generator.generate_section([])
        assert markdown == "No tools found."

    print("✓ test_tools_generator_with_empty_data")


def test_tools_generator_categorization():
    """Test that tools are properly categorized."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = ToolsGenerator(config)

        tools = create_sample_tools_data()
        categorized = generator._categorize_tools(tools)

        assert 'Entity Management' in categorized
        assert 'Graph Operations' in categorized
        assert len(categorized['Entity Management']) == 1
        assert len(categorized['Graph Operations']) == 1
        assert categorized['Entity Management'][0]['name'] == 'memory.create_entities'
        assert categorized['Graph Operations'][0]['name'] == 'memory.read_graph'

    print("✓ test_tools_generator_categorization")


def test_tools_formatter():
    """Test ToolsFormatter utility functions."""
    # Test parameter formatting
    param = {
        'name': 'test_param',
        'type': 'string',
        'description': 'A test parameter',
        'required': True
    }
    formatted = ToolsFormatter.format_parameter(param)
    assert formatted == "`test_param` (string): A test parameter"

    # Test optional parameter
    optional_param = param.copy()
    optional_param['required'] = False
    formatted_optional = ToolsFormatter.format_parameter(optional_param)
    assert ", optional" in formatted_optional

    # Test tool header formatting
    tool = {'name': 'memory.test', 'title': 'Test Tool'}
    header = ToolsFormatter.format_tool_header(tool)
    assert header == "**memory.test** - Test Tool"

    # Test read-only indicator
    assert ToolsFormatter.format_read_only_indicator(True) == "**true**"
    assert ToolsFormatter.format_read_only_indicator(False) == "**false**"

    print("✓ test_tools_formatter")


def test_tools_sorter():
    """Test ToolsSorter utility functions."""
    tools = create_sample_tools_data()

    # Test sorting by name
    sorted_by_name = ToolsSorter.sort_by_name(tools)
    assert sorted_by_name[0]['name'] < sorted_by_name[1]['name']

    # Test sorting by title
    sorted_by_title = ToolsSorter.sort_by_title(tools)
    assert sorted_by_title[0]['title'] < sorted_by_title[1]['title']

    # Test sorting by read-only (non-read-only first)
    sorted_by_readonly = ToolsSorter.sort_by_read_only(tools)
    assert sorted_by_readonly[0]['read_only'] == False
    assert sorted_by_readonly[1]['read_only'] == True

    # Test sorting by parameter count
    sorted_by_params = ToolsSorter.sort_by_parameter_count(tools)
    param_counts = [len(t['parameters']) for t in sorted_by_params]
    assert param_counts == sorted(param_counts)

    print("✓ test_tools_sorter")


def test_tools_validator():
    """Test ToolsValidator utility functions."""
    # Test valid tool
    valid_tool = create_sample_tools_data()[0]
    errors = ToolsValidator.validate_tool(valid_tool)
    assert len(errors) == 0

    # Test invalid tool (missing name)
    invalid_tool = valid_tool.copy()
    del invalid_tool['name']
    errors = ToolsValidator.validate_tool(invalid_tool)
    assert len(errors) > 0
    assert any("Missing or empty required field: name" in error for error in errors)

    # Test tool with invalid name format
    bad_name_tool = valid_tool.copy()
    bad_name_tool['name'] = 'invalid_name'
    errors = ToolsValidator.validate_tool(bad_name_tool)
    assert any("Tool name should start with 'memory.'" in error for error in errors)

    # Test tools list validation
    tools = create_sample_tools_data()
    errors = ToolsValidator.validate_tools_list(tools)
    assert len(errors) == 0

    # Test empty tools list
    errors = ToolsValidator.validate_tools_list([])
    assert any("Tools list is empty" in error for error in errors)

    print("✓ test_tools_validator")


def test_resources_generator_with_sample_data():
    """Test resources generator with sample data."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = ResourcesGenerator(config)

        resources = create_sample_resources_data()
        markdown = generator.generate_section(resources)

        # Check that markdown contains expected content
        assert "<details>" in markdown
        assert "Memory Resources" in markdown
        assert "memory://graph" in markdown
        assert "memory://status" in markdown
        assert "Memory Graph" in markdown
        assert "Memory Status" in markdown

    print("✓ test_resources_generator_with_sample_data")


def test_resources_generator_with_empty_data():
    """Test resources generator with empty data."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = ResourcesGenerator(config)

        markdown = generator.generate_section([])
        assert markdown == "No resources found."

    print("✓ test_resources_generator_with_empty_data")


def test_resources_formatter():
    """Test ResourcesFormatter utility functions."""
    # Test resource header formatting
    resource = {'uri': 'memory://test', 'title': 'Test Resource'}
    header = ResourcesFormatter.format_resource_header(resource)
    assert header == "**memory://test** - Test Resource"

    # Test URI formatting
    assert ResourcesFormatter.format_uri('test') == 'memory://test'
    assert ResourcesFormatter.format_uri('memory://test') == 'memory://test'

    # Test description cleaning
    assert ResourcesFormatter.clean_description('') == 'No description available.'
    assert ResourcesFormatter.clean_description('Test description') == 'Test description.'
    assert ResourcesFormatter.clean_description('Test description.') == 'Test description.'
    assert ResourcesFormatter.clean_description('  Test  description  ') == 'Test description.'

    print("✓ test_resources_formatter")


def test_resources_sorter():
    """Test ResourcesSorter utility functions."""
    resources = create_sample_resources_data()

    # Test sorting by URI
    sorted_by_uri = ResourcesSorter.sort_by_uri(resources)
    uris = [r['uri'] for r in sorted_by_uri]
    assert uris == sorted(uris)

    # Test sorting by title
    sorted_by_title = ResourcesSorter.sort_by_title(resources)
    titles = [r['title'] for r in sorted_by_title]
    assert titles == sorted(titles)

    # Test sorting by resource type
    sorted_by_type = ResourcesSorter.sort_by_resource_type(resources)
    # Should be sorted by the part after 'memory://'
    types = [r['uri'][9:] for r in sorted_by_type]  # Remove 'memory://'
    assert types == sorted(types)

    print("✓ test_resources_sorter")


def test_resources_validator():
    """Test ResourcesValidator utility functions."""
    # Test valid resource
    valid_resource = create_sample_resources_data()[0]
    errors = ResourcesValidator.validate_resource(valid_resource)
    assert len(errors) == 0

    # Test invalid resource (missing URI)
    invalid_resource = valid_resource.copy()
    del invalid_resource['uri']
    errors = ResourcesValidator.validate_resource(invalid_resource)
    assert len(errors) > 0
    assert any("Missing or empty required field: uri" in error for error in errors)

    # Test resource with invalid URI format
    bad_uri_resource = valid_resource.copy()
    bad_uri_resource['uri'] = 'invalid_uri'
    errors = ResourcesValidator.validate_resource(bad_uri_resource)
    assert any("Resource URI should start with 'memory://'" in error for error in errors)

    # Test resources list validation
    resources = create_sample_resources_data()
    errors = ResourcesValidator.validate_resources_list(resources)
    assert len(errors) == 0

    # Test expected resources check
    warnings = ResourcesValidator.check_expected_resources(resources)
    # Should warn about missing memory://types
    assert len(warnings) > 0
    assert any("Missing expected resources" in warning for warning in warnings)

    print("✓ test_resources_validator")


def test_resources_enhancer():
    """Test ResourcesEnhancer utility functions."""
    # Test description enhancement
    resources = [
        {'uri': 'memory://graph', 'title': 'Graph', 'description': ''},
        {'uri': 'memory://status', 'title': 'Status', 'description': 'Custom description'}
    ]

    enhanced = ResourcesEnhancer.enhance_descriptions(resources)

    # First resource should get generated description
    assert enhanced[0]['description'] != ''
    assert "knowledge graph" in enhanced[0]['description'].lower()

    # Second resource should keep custom description (cleaned)
    assert enhanced[1]['description'] == 'Custom description.'

    # Test metadata addition
    with_metadata = ResourcesEnhancer.add_metadata(resources)
    assert with_metadata[0]['resource_type'] == 'graph'
    assert with_metadata[1]['resource_type'] == 'status'
    assert 'content_hint' in with_metadata[0]

    print("✓ test_resources_enhancer")


def test_generators_integration():
    """Test integration between tools and resources generators."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)

        tools_generator = ToolsGenerator(config)
        resources_generator = ResourcesGenerator(config)

        tools = create_sample_tools_data()
        resources = create_sample_resources_data()

        tools_markdown = tools_generator.generate_section(tools)
        resources_markdown = resources_generator.generate_section(resources)

        # Both should generate valid markdown
        assert len(tools_markdown) > 0
        assert len(resources_markdown) > 0

        # Both should contain the generation comment
        assert "<!-- NOTE: This has been generated via update-docs.py --->" in tools_markdown
        assert "<!-- NOTE: This has been generated via update-docs.py --->" in resources_markdown

        # Combine them (like the main generator would)
        combined = tools_markdown + "\n\n" + resources_markdown
        assert "Entity Management" in combined
        assert "Memory Resources" in combined

    print("✓ test_generators_integration")


def run_all_tests():
    """Run all test functions."""
    test_functions = [
        test_tools_generator_with_sample_data,
        test_tools_generator_with_empty_data,
        test_tools_generator_categorization,
        test_tools_formatter,
        test_tools_sorter,
        test_tools_validator,
        test_resources_generator_with_sample_data,
        test_resources_generator_with_empty_data,
        test_resources_formatter,
        test_resources_sorter,
        test_resources_validator,
        test_resources_enhancer,
        test_generators_integration,
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
