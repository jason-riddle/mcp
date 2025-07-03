"""
Simple test runner for Java extractor.
"""

import sys
import json
import tempfile
from pathlib import Path
from unittest.mock import Mock, patch

# Add parent directory to path to import our modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from update_docs.config import DocumentationConfig
from update_docs.extractors.java_extractor import (
    JavaReflectionExtractor, JavaClasspathBuilder, JavaProcessManager
)
from update_docs.extractors.base import ExtractionError


def create_test_project_structure(temp_dir: Path) -> Path:
    """Create a test project structure."""
    # Create basic structure
    src_dir = temp_dir / "src" / "main" / "java" / "com" / "jasonriddle" / "mcp"
    src_dir.mkdir(parents=True)

    # Create target directory
    target_dir = temp_dir / "target" / "classes"
    target_dir.mkdir(parents=True)

    # Create scripts directory
    scripts_dir = temp_dir / "scripts"
    scripts_dir.mkdir()

    # Create README
    (temp_dir / "README.md").write_text("# Test README")

    # Create test Java files
    tools_content = '''
    @Tool(name = "memory.test", description = "Test tool")
    public String testMethod() { return "test"; }
    '''
    (src_dir / "McpMemoryTools.java").write_text(tools_content)

    resources_content = '''
    @Resource(uri = "memory://test")
    TextResourceContents testResource() { return null; }
    '''
    (src_dir / "McpMemoryResources.java").write_text(resources_content)

    return temp_dir


def test_java_extractor_initialization():
    """Test Java extractor initialization."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)

        # Test with fallback enabled
        extractor = JavaReflectionExtractor(config, fallback_to_regex=True)
        assert extractor.fallback_to_regex == True
        assert extractor._regex_extractor is None  # Lazy initialization

        # Test with fallback disabled
        extractor_no_fallback = JavaReflectionExtractor(config, fallback_to_regex=False)
        assert extractor_no_fallback.fallback_to_regex == False

    print("✓ test_java_extractor_initialization")


def test_java_extractor_lazy_regex_initialization():
    """Test lazy initialization of regex extractor."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = JavaReflectionExtractor(config)

        # Should be None initially
        assert extractor._regex_extractor is None

        # Should initialize on first access
        regex_extractor = extractor.regex_extractor
        assert regex_extractor is not None
        assert extractor._regex_extractor is not None

        # Should return same instance on subsequent access
        assert extractor.regex_extractor is regex_extractor

    print("✓ test_java_extractor_lazy_regex_initialization")


def test_java_extractor_build_command():
    """Test Java command building."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        # Create quarkus lib directory
        quarkus_lib = temp_path / "target" / "quarkus-app" / "lib" / "main"
        quarkus_lib.mkdir(parents=True)
        (quarkus_lib / "test.jar").write_text("test")

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = JavaReflectionExtractor(config)

        cmd = extractor._build_java_command()

        assert cmd[0] == "java"
        assert "-cp" in cmd
        assert "UpdateReadme" in cmd
        assert "--json" in cmd

        # Check classpath includes expected components
        cp_index = cmd.index("-cp")
        classpath = cmd[cp_index + 1]
        assert "scripts" in classpath
        assert "target/classes" in classpath
        assert "target/quarkus-app/lib/main/*" in classpath

    print("✓ test_java_extractor_build_command")


def test_java_extractor_fallback_to_regex():
    """Test fallback to regex extraction when Java fails."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = JavaReflectionExtractor(config, fallback_to_regex=True)

        # Mock subprocess to simulate Java failure
        with patch('subprocess.run') as mock_run:
            mock_run.side_effect = Exception("Java failed")

            # Should fallback to regex without raising exception
            tools = extractor.extract_tools()
            resources = extractor.extract_resources()
            data = extractor.extract_all()

            # Should return data (from regex fallback)
            assert isinstance(tools, list)
            assert isinstance(resources, list)
            assert isinstance(data, dict)
            assert 'tools' in data
            assert 'resources' in data

    print("✓ test_java_extractor_fallback_to_regex")


def test_java_extractor_no_fallback_raises_error():
    """Test that extraction raises error when fallback is disabled."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = JavaReflectionExtractor(config, fallback_to_regex=False)

        # Mock subprocess to simulate Java failure
        with patch('subprocess.run') as mock_run:
            mock_run.side_effect = Exception("Java failed")

            # Should raise ExtractionError
            try:
                extractor.extract_tools()
                assert False, "Should have raised ExtractionError"
            except ExtractionError:
                pass  # Expected

    print("✓ test_java_extractor_no_fallback_raises_error")


def test_java_extractor_successful_extraction():
    """Test successful Java extraction with mocked subprocess."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = JavaReflectionExtractor(config)

        # Mock successful Java output
        mock_output = {
            "tools": [
                {
                    "name": "memory.test_tool",
                    "description": "A test tool",
                    "method_name": "testTool",
                    "return_type": "String",
                    "parameters": [
                        {
                            "name": "param1",
                            "type": "String",
                            "description": "Test parameter",
                            "required": True
                        }
                    ]
                }
            ],
            "resources": [
                {
                    "uri": "memory://test",
                    "method_name": "testResource",
                    "description": "Test resource"
                }
            ]
        }

        with patch('subprocess.run') as mock_run:
            mock_run.return_value.returncode = 0
            mock_run.return_value.stdout = json.dumps(mock_output)
            mock_run.return_value.stderr = ""

            data = extractor.extract_all()

            assert len(data['tools']) == 1
            assert len(data['resources']) == 1

            tool = data['tools'][0]
            assert tool['name'] == 'memory.test_tool'
            assert tool['description'] == 'A test tool'
            assert tool['method_name'] == 'testTool'
            assert len(tool['parameters']) == 1

            resource = data['resources'][0]
            assert resource['uri'] == 'memory://test'
            assert resource['method_name'] == 'testResource'

    print("✓ test_java_extractor_successful_extraction")


def test_java_extractor_invalid_json_output():
    """Test handling of invalid JSON output from Java."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = JavaReflectionExtractor(config, fallback_to_regex=False)

        with patch('subprocess.run') as mock_run:
            mock_run.return_value.returncode = 0
            mock_run.return_value.stdout = "invalid json"
            mock_run.return_value.stderr = ""

            try:
                extractor.extract_all()
                assert False, "Should have raised ExtractionError"
            except ExtractionError as e:
                assert "Failed to parse Java output as JSON" in str(e)

    print("✓ test_java_extractor_invalid_json_output")


def test_java_extractor_missing_keys_in_output():
    """Test handling of missing required keys in Java output."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = JavaReflectionExtractor(config, fallback_to_regex=False)

        with patch('subprocess.run') as mock_run:
            mock_run.return_value.returncode = 0
            mock_run.return_value.stdout = '{"tools": []}'  # Missing resources
            mock_run.return_value.stderr = ""

            try:
                extractor.extract_all()
                assert False, "Should have raised ExtractionError"
            except ExtractionError as e:
                assert "missing expected 'tools' and 'resources' keys" in str(e)

    print("✓ test_java_extractor_missing_keys_in_output")


def test_java_extractor_check_availability():
    """Test Java availability checking."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = JavaReflectionExtractor(config)

        checks = extractor.check_java_availability()

        # Should include all expected keys
        expected_keys = [
            'java_available',
            'classes_compiled',
            'update_readme_exists',
            'tools_file_exists',
            'resources_file_exists'
        ]

        for key in expected_keys:
            assert key in checks
            assert isinstance(checks[key], bool)

    print("✓ test_java_extractor_check_availability")


def test_java_classpath_builder():
    """Test JavaClasspathBuilder functionality."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        # Create additional directories
        (temp_path / "target" / "dependency").mkdir(parents=True)
        (temp_path / "target" / "lib").mkdir(parents=True)
        (temp_path / "target" / "lib" / "test.jar").write_text("test")

        builder = JavaClasspathBuilder(temp_path)

        classpath = builder.build_classpath()
        assert "scripts" in classpath
        assert "target/classes" in classpath

        jar_files = builder.find_jar_files()
        assert len(jar_files) >= 1
        assert any("test.jar" in str(jar) for jar in jar_files)

    print("✓ test_java_classpath_builder")


def test_java_process_manager_execute_command():
    """Test JavaProcessManager command execution."""
    with patch('subprocess.run') as mock_run:
        mock_run.return_value.returncode = 0
        mock_run.return_value.stdout = "success"
        mock_run.return_value.stderr = ""

        result = JavaProcessManager.execute_java_command(
            ["echo", "test"], Path.cwd(), timeout=10
        )

        assert result.returncode == 0
        assert result.stdout == "success"

    print("✓ test_java_process_manager_execute_command")


def test_java_process_manager_validate_output():
    """Test JavaProcessManager output validation."""
    # Test valid output
    valid_output = '{"tools": [], "resources": []}'
    data = JavaProcessManager.validate_java_output(valid_output)
    assert 'tools' in data
    assert 'resources' in data

    # Test invalid JSON
    try:
        JavaProcessManager.validate_java_output("invalid json")
        assert False, "Should have raised ExtractionError"
    except ExtractionError:
        pass  # Expected

    # Test missing keys
    try:
        JavaProcessManager.validate_java_output('{"tools": []}')
        assert False, "Should have raised ExtractionError"
    except ExtractionError:
        pass  # Expected

    print("✓ test_java_process_manager_validate_output")


def test_java_extractor_process_tool_data():
    """Test processing of individual tool data."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = JavaReflectionExtractor(config)

        tool_data = {
            "name": "memory.test_tool",
            "description": "A test tool",
            "method_name": "testTool",
            "return_type": "String",
            "parameters": [
                {
                    "name": "param1",
                    "type": "List<String>",
                    "description": "Test parameter",
                    "required": True
                }
            ]
        }

        processed = extractor._process_tool_data(tool_data)

        assert processed['name'] == 'memory.test_tool'
        assert processed['title'] == 'Test Tool'
        assert len(processed['parameters']) == 1
        assert processed['parameters'][0]['type'] == 'array'  # Simplified type

    print("✓ test_java_extractor_process_tool_data")


def test_java_extractor_process_resource_data():
    """Test processing of individual resource data."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        create_test_project_structure(temp_path)

        config = DocumentationConfig.from_project_root(temp_path)
        extractor = JavaReflectionExtractor(config)

        resource_data = {
            "uri": "memory://test",
            "method_name": "testResource",
            "description": "Test resource"
        }

        processed = extractor._process_resource_data(resource_data)

        assert processed['uri'] == 'memory://test'
        assert processed['title'] == 'Memory Test'
        assert processed['method_name'] == 'testResource'
        assert processed['description'] == 'Test resource'

    print("✓ test_java_extractor_process_resource_data")


def run_all_tests():
    """Run all test functions."""
    test_functions = [
        test_java_extractor_initialization,
        test_java_extractor_lazy_regex_initialization,
        test_java_extractor_build_command,
        test_java_extractor_fallback_to_regex,
        test_java_extractor_no_fallback_raises_error,
        test_java_extractor_successful_extraction,
        test_java_extractor_invalid_json_output,
        test_java_extractor_missing_keys_in_output,
        test_java_extractor_check_availability,
        test_java_classpath_builder,
        test_java_process_manager_execute_command,
        test_java_process_manager_validate_output,
        test_java_extractor_process_tool_data,
        test_java_extractor_process_resource_data,
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
