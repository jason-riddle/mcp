"""
Simple test runner for base extractor classes.
"""

import sys
import tempfile
from pathlib import Path

# Add parent directory to path to import our modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from update_docs.config import DocumentationConfig
from update_docs.extractors.base import (
    DataExtractor, ParameterInfo, ToolInfo, ResourceInfo, ExtractionError
)


class MockDataExtractor(DataExtractor):
    """Mock implementation of DataExtractor for testing."""

    def extract_tools(self):
        return [
            {
                'name': 'memory.test_tool',
                'title': 'Test Tool',
                'description': 'A test tool',
                'method_name': 'testTool',
                'return_type': 'String',
                'parameters': [],
                'read_only': False
            }
        ]

    def extract_resources(self):
        return [
            {
                'uri': 'memory://test',
                'title': 'Memory Test',
                'description': 'A test resource',
                'method_name': 'testResource'
            }
        ]

    def extract_prompts(self):
        return [
            {
                'name': 'test_prompt',
                'title': 'Test Prompt',
                'description': 'A test prompt',
                'arguments': []
            }
        ]


def test_data_extractor_abstract_methods():
    """Test that DataExtractor is abstract."""
    config = DocumentationConfig.from_project_root(Path.cwd())

    # Should not be able to instantiate abstract class directly
    try:
        DataExtractor(config)
        assert False, "Should not be able to instantiate abstract class"
    except TypeError:
        pass  # Expected
    print("✓ test_data_extractor_abstract_methods")


def test_mock_extractor_implementation():
    """Test mock extractor implementation."""
    config = DocumentationConfig.from_project_root(Path.cwd())
    extractor = MockDataExtractor(config)

    tools = extractor.extract_tools()
    assert len(tools) == 1
    assert tools[0]['name'] == 'memory.test_tool'

    resources = extractor.extract_resources()
    assert len(resources) == 1
    assert resources[0]['uri'] == 'memory://test'

    prompts = extractor.extract_prompts()
    assert len(prompts) == 1
    assert prompts[0]['name'] == 'test_prompt'
    print("✓ test_mock_extractor_implementation")


def test_extract_all_method():
    """Test extract_all method returns tools, resources, and prompts."""
    config = DocumentationConfig.from_project_root(Path.cwd())
    extractor = MockDataExtractor(config)

    data = extractor.extract_all()

    assert 'tools' in data
    assert 'resources' in data
    assert 'prompts' in data
    assert len(data['tools']) == 1
    assert len(data['resources']) == 1
    assert len(data['prompts']) == 1
    print("✓ test_extract_all_method")


def test_format_tool_title():
    """Test tool title formatting."""
    config = DocumentationConfig.from_project_root(Path.cwd())
    extractor = MockDataExtractor(config)

    title = extractor.format_tool_title("memory.create_entities")
    assert title == "Create Entities"

    title = extractor.format_tool_title("memory.search_nodes")
    assert title == "Search Nodes"
    print("✓ test_format_tool_title")


def test_format_resource_title():
    """Test resource title formatting."""
    config = DocumentationConfig.from_project_root(Path.cwd())
    extractor = MockDataExtractor(config)

    title = extractor.format_resource_title("memory://graph")
    assert title == "Memory Graph"

    title = extractor.format_resource_title("memory://status")
    assert title == "Memory Status"
    print("✓ test_format_resource_title")


def test_is_read_only_tool():
    """Test read-only tool detection."""
    config = DocumentationConfig.from_project_root(Path.cwd())
    extractor = MockDataExtractor(config)

    # Non-String return type should be read-only
    assert extractor.is_read_only_tool("memory.test", "List<String>") == True

    # String return type with read/search/open in name should be read-only
    assert extractor.is_read_only_tool("memory.read_graph", "String") == True
    assert extractor.is_read_only_tool("memory.search_nodes", "String") == True
    assert extractor.is_read_only_tool("memory.open_nodes", "String") == True

    # String return type without read/search/open should not be read-only
    assert extractor.is_read_only_tool("memory.create_entities", "String") == False
    print("✓ test_is_read_only_tool")


def test_parameter_info():
    """Test ParameterInfo class."""
    param = ParameterInfo("testParam", "string", "A test parameter", True)

    assert param.name == "testParam"
    assert param.param_type == "string"
    assert param.description == "A test parameter"
    assert param.required == True

    param_dict = param.to_dict()
    expected = {
        'name': 'testParam',
        'type': 'string',
        'description': 'A test parameter',
        'required': True
    }
    assert param_dict == expected
    print("✓ test_parameter_info")


def test_parameter_info_optional():
    """Test ParameterInfo with optional parameter."""
    param = ParameterInfo("optionalParam", "string", "An optional parameter", False)

    assert param.required == False

    param_dict = param.to_dict()
    assert param_dict['required'] == False
    print("✓ test_parameter_info_optional")


def test_tool_info():
    """Test ToolInfo class."""
    params = [
        ParameterInfo("param1", "string", "First parameter"),
        ParameterInfo("param2", "array", "Second parameter")
    ]

    tool = ToolInfo("memory.test_tool", "A test tool", "testTool", "String", params)

    assert tool.name == "memory.test_tool"
    assert tool.description == "A test tool"
    assert tool.method_name == "testTool"
    assert tool.return_type == "String"
    assert len(tool.parameters) == 2
    print("✓ test_tool_info")


def test_tool_info_to_dict():
    """Test ToolInfo to_dict conversion."""
    config = DocumentationConfig.from_project_root(Path.cwd())
    params = [ParameterInfo("param1", "string", "First parameter")]

    tool = ToolInfo("memory.create_entities", "Create entities", "createEntities", "String", params)
    tool_dict = tool.to_dict(config)

    assert tool_dict['name'] == "memory.create_entities"
    assert tool_dict['title'] == "Create Entities"
    assert tool_dict['description'] == "Create entities"
    assert tool_dict['method_name'] == "createEntities"
    assert tool_dict['return_type'] == "String"
    assert tool_dict['read_only'] == False
    assert len(tool_dict['parameters']) == 1
    print("✓ test_tool_info_to_dict")


def test_resource_info():
    """Test ResourceInfo class."""
    resource = ResourceInfo("memory://graph", "getGraph", "Graph resource")

    assert resource.uri == "memory://graph"
    assert resource.method_name == "getGraph"
    assert resource.description == "Graph resource"
    print("✓ test_resource_info")


def test_resource_info_to_dict():
    """Test ResourceInfo to_dict conversion."""
    resource = ResourceInfo("memory://status", "getStatus", "Status resource")
    resource_dict = resource.to_dict()

    assert resource_dict['uri'] == "memory://status"
    assert resource_dict['title'] == "Memory Status"
    assert resource_dict['description'] == "Status resource"
    assert resource_dict['method_name'] == "getStatus"
    print("✓ test_resource_info_to_dict")


def test_resource_info_no_description():
    """Test ResourceInfo with no description generates default."""
    resource = ResourceInfo("memory://types", "getTypes")
    resource_dict = resource.to_dict()

    assert resource_dict['description'] == "Resource for memory types"
    print("✓ test_resource_info_no_description")


def test_extraction_error():
    """Test ExtractionError exception."""
    try:
        raise ExtractionError("Test extraction error")
    except ExtractionError as e:
        assert str(e) == "Test extraction error"
    print("✓ test_extraction_error")


def run_all_tests():
    """Run all test functions."""
    test_functions = [
        test_data_extractor_abstract_methods,
        test_mock_extractor_implementation,
        test_extract_all_method,
        test_format_tool_title,
        test_format_resource_title,
        test_is_read_only_tool,
        test_parameter_info,
        test_parameter_info_optional,
        test_tool_info,
        test_tool_info_to_dict,
        test_resource_info,
        test_resource_info_to_dict,
        test_resource_info_no_description,
        test_extraction_error,
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
