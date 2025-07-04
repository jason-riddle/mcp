"""
Simple test runner for prompts generator.
"""

import sys
import tempfile
from pathlib import Path

# Add parent directory to path to import our modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from update_docs.config import DocumentationConfig
from update_docs.generators.prompts_generator import (
    PromptsGenerator, PromptsFormatter, PromptsSorter, PromptsValidator, PromptsEnhancer
)


def create_sample_prompts_data():
    """Create sample prompts data for testing."""
    return [
        {
            'name': 'memory.guidance',
            'title': 'Memory Guidance',
            'description': 'Provides guidance for working with the memory graph system',
            'arguments': [
                {
                    'name': 'query_type',
                    'type': 'string',
                    'description': 'Type of query to help with',
                    'required': True
                },
                {
                    'name': 'context',
                    'type': 'string',
                    'description': 'Additional context for the guidance',
                    'required': False
                }
            ]
        },
        {
            'name': 'memory.search_helper',
            'title': 'Search Helper',
            'description': 'Assists with searching and analyzing data within the system',
            'arguments': []
        }
    ]


def test_prompts_generator_initialization():
    """Test PromptsGenerator initialization."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = PromptsGenerator(config)

        assert generator.config == config

    print("✓ test_prompts_generator_initialization")


def test_generate_section_with_sample_data():
    """Test prompts generator with sample data."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = PromptsGenerator(config)

        prompts = create_sample_prompts_data()
        markdown = generator.generate_section(prompts)

        # Check that markdown contains expected sections
        assert "<details>" in markdown
        assert "Memory Guidance" in markdown
        assert "memory.guidance" in markdown
        assert "memory.search_helper" in markdown
        assert "query_type" in markdown
        assert "optional" in markdown

    print("✓ test_generate_section_with_sample_data")


def test_generate_section_with_empty_data():
    """Test prompts generator with empty data."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = PromptsGenerator(config)

        markdown = generator.generate_section([])
        assert markdown == "No prompts found."

    print("✓ test_generate_section_with_empty_data")


def test_categorize_prompts():
    """Test that prompts are properly categorized."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = PromptsGenerator(config)

        prompts = create_sample_prompts_data()
        categorized = generator._categorize_prompts(prompts)

        # Both should be in Memory Guidance based on current logic
        assert 'Memory Guidance' in categorized
        # The search helper should also be Memory Guidance since it has 'memory' in name
        assert len(categorized['Memory Guidance']) == 2
        names = [p['name'] for p in categorized['Memory Guidance']]
        assert 'memory.guidance' in names
        assert 'memory.search_helper' in names

    print("✓ test_categorize_prompts")


def test_determine_prompt_category():
    """Test category determination logic."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = PromptsGenerator(config)

        # Test memory guidance category
        memory_prompt = {'name': 'memory.guidance', 'description': 'memory guidance for users'}
        assert generator._determine_prompt_category(memory_prompt) == 'Memory Guidance'

        # Test graph operations category (name without 'memory' prefix)
        graph_prompt = {'name': 'graph_helper', 'description': 'helps with entity operations'}
        assert generator._determine_prompt_category(graph_prompt) == 'Graph Operations'

        # Test data analysis category (name without 'memory' prefix)
        search_prompt = {'name': 'search_helper', 'description': 'helps analyze search results'}
        assert generator._determine_prompt_category(search_prompt) == 'Data Analysis'

        # Test other category (name without specific keywords)
        other_prompt = {'name': 'unknown_prompt', 'description': 'some other functionality'}
        assert generator._determine_prompt_category(other_prompt) == 'Other'

    print("✓ test_determine_prompt_category")


def test_format_prompt():
    """Test individual prompt formatting."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = PromptsGenerator(config)

        prompt = create_sample_prompts_data()[0]
        formatted = generator._format_prompt(prompt)

        formatted_text = '\n'.join(formatted)
        assert "<!-- NOTE: This has been generated via update-docs.py" in formatted_text
        assert "**memory.guidance**" in formatted_text
        assert "Memory Guidance" in formatted_text
        assert "query_type" in formatted_text
        assert "context" in formatted_text
        assert "optional" in formatted_text

    print("✓ test_format_prompt")


def test_prompts_formatter_format_argument():
    """Test PromptsFormatter argument formatting."""
    # Test required argument
    required_arg = {
        'name': 'test_param',
        'type': 'string',
        'description': 'A test parameter',
        'required': True
    }
    formatted = PromptsFormatter.format_argument(required_arg)
    assert formatted == "`test_param` (string): A test parameter"

    # Test optional argument
    optional_arg = required_arg.copy()
    optional_arg['required'] = False
    formatted_optional = PromptsFormatter.format_argument(optional_arg)
    assert ", optional" in formatted_optional

    print("✓ test_prompts_formatter_format_argument")


def test_prompts_formatter_format_prompt_header():
    """Test PromptsFormatter prompt header formatting."""
    prompt = {'name': 'memory.test', 'title': 'Test Prompt'}
    header = PromptsFormatter.format_prompt_header(prompt)
    assert header == "**memory.test** - Test Prompt"

    # Test with missing title
    prompt_no_title = {'name': 'memory.test'}
    header_no_title = PromptsFormatter.format_prompt_header(prompt_no_title)
    assert header_no_title == "**memory.test** - No title"

    print("✓ test_prompts_formatter_format_prompt_header")


def test_prompts_formatter_clean_description():
    """Test PromptsFormatter description cleaning."""
    # Test empty description
    assert PromptsFormatter.clean_description('') == "No description available."

    # Test description without period
    assert PromptsFormatter.clean_description('Test description') == "Test description."

    # Test description with period
    assert PromptsFormatter.clean_description('Test description.') == "Test description."

    # Test description with extra whitespace
    assert PromptsFormatter.clean_description('  Test  description  ') == "Test description."

    print("✓ test_prompts_formatter_clean_description")


def test_prompts_formatter_escape_markdown():
    """Test PromptsFormatter markdown escaping."""
    text_with_markdown = "Text with *bold* and `code` and [link]"
    escaped = PromptsFormatter.escape_markdown(text_with_markdown)

    assert r'\*' in escaped
    assert r'\`' in escaped
    assert r'\[' in escaped
    assert r'\]' in escaped

    print("✓ test_prompts_formatter_escape_markdown")


def test_prompts_sorter_sort_by_name():
    """Test PromptsSorter name sorting."""
    prompts = create_sample_prompts_data()
    sorted_prompts = PromptsSorter.sort_by_name(prompts)

    names = [p['name'] for p in sorted_prompts]
    assert names == sorted(names)

    print("✓ test_prompts_sorter_sort_by_name")


def test_prompts_sorter_sort_by_title():
    """Test PromptsSorter title sorting."""
    prompts = create_sample_prompts_data()
    sorted_prompts = PromptsSorter.sort_by_title(prompts)

    titles = [p['title'] for p in sorted_prompts]
    assert titles == sorted(titles)

    print("✓ test_prompts_sorter_sort_by_title")


def test_prompts_sorter_sort_by_argument_count():
    """Test PromptsSorter argument count sorting."""
    prompts = create_sample_prompts_data()
    sorted_prompts = PromptsSorter.sort_by_argument_count(prompts)

    arg_counts = [len(p.get('arguments', [])) for p in sorted_prompts]
    assert arg_counts == sorted(arg_counts)

    print("✓ test_prompts_sorter_sort_by_argument_count")


def test_prompts_validator_validate_prompt():
    """Test PromptsValidator prompt validation."""
    # Test valid prompt
    valid_prompt = create_sample_prompts_data()[0]
    errors = PromptsValidator.validate_prompt(valid_prompt)
    assert len(errors) == 0

    # Test invalid prompt (missing name)
    invalid_prompt = valid_prompt.copy()
    del invalid_prompt['name']
    errors = PromptsValidator.validate_prompt(invalid_prompt)
    assert len(errors) > 0
    assert any("Missing or empty required field: name" in error for error in errors)

    # Test prompt with invalid name
    bad_name_prompt = valid_prompt.copy()
    bad_name_prompt['name'] = ''
    errors = PromptsValidator.validate_prompt(bad_name_prompt)
    assert any("Missing or empty required field: name" in error for error in errors)

    print("✓ test_prompts_validator_validate_prompt")


def test_prompts_validator_validate_argument():
    """Test PromptsValidator argument validation."""
    # Test valid argument
    valid_arg = {
        'name': 'test_arg',
        'type': 'string',
        'description': 'Test argument',
        'required': True
    }
    errors = PromptsValidator.validate_argument(valid_arg, 0)
    assert len(errors) == 0

    # Test invalid argument (missing name)
    invalid_arg = valid_arg.copy()
    del invalid_arg['name']
    errors = PromptsValidator.validate_argument(invalid_arg, 0)
    assert len(errors) > 0
    assert any("Missing or empty required field: name" in error for error in errors)

    # Test argument with invalid name format
    bad_name_arg = valid_arg.copy()
    bad_name_arg['name'] = 'invalid@name'
    errors = PromptsValidator.validate_argument(bad_name_arg, 0)
    assert any("Invalid name format" in error for error in errors)

    print("✓ test_prompts_validator_validate_argument")


def test_prompts_validator_validate_prompts_list():
    """Test PromptsValidator prompts list validation."""
    # Test valid prompts list
    prompts = create_sample_prompts_data()
    errors = PromptsValidator.validate_prompts_list(prompts)
    assert len(errors) == 0

    # Test empty prompts list
    errors = PromptsValidator.validate_prompts_list([])
    assert any("Prompts list is empty" in error for error in errors)

    # Test duplicate names
    duplicate_prompts = prompts + [prompts[0]]  # Add duplicate
    errors = PromptsValidator.validate_prompts_list(duplicate_prompts)
    assert any("Duplicate prompt names found" in error for error in errors)

    print("✓ test_prompts_validator_validate_prompts_list")


def test_prompts_enhancer_enhance_descriptions():
    """Test PromptsEnhancer description enhancement."""
    # Test prompts with empty descriptions
    prompts = [
        {'name': 'memory.guidance', 'description': ''},
        {'name': 'memory.search', 'description': 'Custom description'}
    ]

    enhanced = PromptsEnhancer.enhance_descriptions(prompts)

    # First prompt should get generated description
    assert enhanced[0]['description'] != ''
    assert "memory" in enhanced[0]['description'].lower()

    # Second prompt should keep custom description (cleaned)
    assert enhanced[1]['description'] == 'Custom description.'

    print("✓ test_prompts_enhancer_enhance_descriptions")


def test_prompts_enhancer_generate_description():
    """Test PromptsEnhancer description generation."""
    # Test memory-related prompt
    memory_desc = PromptsEnhancer._generate_description({'name': 'memory.guidance', 'arguments': []})
    assert "memory graph" in memory_desc.lower()

    # Test graph-related prompt (name without 'memory' prefix to trigger graph logic)
    graph_desc = PromptsEnhancer._generate_description({'name': 'graph_helper', 'arguments': []})
    assert "graph operations" in graph_desc.lower()

    # Test search-related prompt (name without 'memory' prefix to trigger search logic)
    search_desc = PromptsEnhancer._generate_description({'name': 'search_helper', 'arguments': []})
    assert "searching" in search_desc.lower()

    # Test prompt with arguments (name without specific keywords to trigger args logic)
    args_desc = PromptsEnhancer._generate_description({'name': 'test_prompt', 'arguments': [{}]})
    assert "1 argument" in args_desc

    print("✓ test_prompts_enhancer_generate_description")


def test_prompts_enhancer_add_metadata():
    """Test PromptsEnhancer metadata addition."""
    prompts = create_sample_prompts_data()

    enhanced = PromptsEnhancer.add_metadata(prompts)

    # Check argument count metadata
    assert enhanced[0]['argument_count'] == 2
    assert enhanced[1]['argument_count'] == 0

    # Check complexity indicators
    assert enhanced[0]['complexity'] == 'Moderate'
    assert enhanced[1]['complexity'] == 'Simple'

    # Check usage hints
    assert 'usage_hint' in enhanced[0]
    assert 'usage_hint' in enhanced[1]

    print("✓ test_prompts_enhancer_add_metadata")


def test_prompts_enhancer_complexity_levels():
    """Test PromptsEnhancer complexity level determination."""
    # Simple (no arguments)
    simple_prompt = {'arguments': []}
    assert PromptsEnhancer._get_complexity_level(simple_prompt) == "Simple"

    # Moderate (1-3 arguments)
    moderate_prompt = {'arguments': [{}]}
    assert PromptsEnhancer._get_complexity_level(moderate_prompt) == "Moderate"

    # Complex (4+ arguments)
    complex_prompt = {'arguments': [{}, {}, {}, {}]}
    assert PromptsEnhancer._get_complexity_level(complex_prompt) == "Complex"

    print("✓ test_prompts_enhancer_complexity_levels")


def test_prompts_enhancer_usage_hints():
    """Test PromptsEnhancer usage hint generation."""
    # Memory-related prompt
    memory_hint = PromptsEnhancer._get_usage_hint({'name': 'memory.guidance', 'arguments': []})
    assert "memory system" in memory_hint.lower()

    # Graph-related prompt (name without 'memory' prefix to trigger graph logic)
    graph_hint = PromptsEnhancer._get_usage_hint({'name': 'graph_helper', 'arguments': []})
    assert "graph operations" in graph_hint.lower()

    # Prompt with arguments (name without 'memory' to trigger arguments logic)
    args_hint = PromptsEnhancer._get_usage_hint({'name': 'test_prompt', 'arguments': [{}]})
    assert "1 input" in args_hint

    print("✓ test_prompts_enhancer_usage_hints")


def test_prompts_generator_integration():
    """Test integration of prompts generator with sample data."""
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_path = Path(temp_dir)
        config = DocumentationConfig.from_project_root(temp_path)
        generator = PromptsGenerator(config)

        prompts = create_sample_prompts_data()
        markdown = generator.generate_section(prompts)

        # Should generate valid markdown
        assert len(markdown) > 0
        assert markdown != "No prompts found."

        # Should contain the generation comment
        assert "<!-- NOTE: This has been generated via update-docs.py" in markdown

        # Should contain both prompts
        assert "memory.guidance" in markdown
        assert "memory.search_helper" in markdown

        # Should have proper structure
        assert "<details>" in markdown
        assert "</details>" in markdown
        assert "<summary>" in markdown
        assert "</summary>" in markdown

    print("✓ test_prompts_generator_integration")


def run_all_tests():
    """Run all test functions."""
    test_functions = [
        test_prompts_generator_initialization,
        test_generate_section_with_sample_data,
        test_generate_section_with_empty_data,
        test_categorize_prompts,
        test_determine_prompt_category,
        test_format_prompt,
        test_prompts_formatter_format_argument,
        test_prompts_formatter_format_prompt_header,
        test_prompts_formatter_clean_description,
        test_prompts_formatter_escape_markdown,
        test_prompts_sorter_sort_by_name,
        test_prompts_sorter_sort_by_title,
        test_prompts_sorter_sort_by_argument_count,
        test_prompts_validator_validate_prompt,
        test_prompts_validator_validate_argument,
        test_prompts_validator_validate_prompts_list,
        test_prompts_enhancer_enhance_descriptions,
        test_prompts_enhancer_generate_description,
        test_prompts_enhancer_add_metadata,
        test_prompts_enhancer_complexity_levels,
        test_prompts_enhancer_usage_hints,
        test_prompts_generator_integration,
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
