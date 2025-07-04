"""
Simple test runner for markdown validator.
"""

import sys
import tempfile
from pathlib import Path

# Add parent directory to path to import our modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from update_docs.validators.markdown_validator import (
    MarkdownValidator, SectionValidator, ContentEnhancer, ValidationResult
)


def create_valid_tools_markdown():
    """Create valid tools markdown for testing."""
    return """<details>
<summary><b>Entity Management</b></summary>

<!-- NOTE: This has been generated via update-docs.py --->

- **memory.create_entities**
  - Title: Create Entities
  - Description: Create multiple new entities in the knowledge graph.
  - Parameters:
    - `entities` (array): Array of entities with name, entityType, and observations
  - Read-only: **false**

</details>

<details>
<summary><b>Graph Operations</b></summary>

<!-- NOTE: This has been generated via update-docs.py --->

- **memory.read_graph**
  - Title: Read Graph
  - Description: Read the entire knowledge graph.
  - Parameters: None
  - Read-only: **true**

</details>
"""


def create_valid_resources_markdown():
    """Create valid resources markdown for testing."""
    return """<details>
<summary><b>Memory Resources</b></summary>

<!-- NOTE: This has been generated via update-docs.py --->

- **memory://graph**
  - Title: Memory Graph
  - Description: Returns the complete knowledge graph in formatted way.

- **memory://status**
  - Title: Memory Status
  - Description: Returns memory graph status information.

</details>
"""


def create_invalid_markdown():
    """Create invalid markdown for testing."""
    return """<details>
<summary><b>Broken Section</b>

<!-- Missing closing summary tag -->

- **memory.broken_tool
  - Title: Broken Tool
  - Description: Missing closing bold tag
  - Parameters
    - broken_param (string): Missing backticks

<!-- Missing closing details tag -->
"""


def test_markdown_validator_initialization():
    """Test MarkdownValidator initialization."""
    validator = MarkdownValidator()
    assert validator is not None
    print("✓ test_markdown_validator_initialization")


def test_validate_valid_tools_markdown():
    """Test validation of valid tools markdown."""
    validator = MarkdownValidator()
    content = create_valid_tools_markdown()

    result = validator.validate_markdown(content)

    assert isinstance(result, ValidationResult)
    assert result.is_valid == True
    assert len(result.errors) == 0

    # Check content stats
    assert result.content_stats['tools_count'] == 2
    assert result.content_stats['details_blocks'] == 2
    assert result.content_stats['generation_comments'] == 2

    print("✓ test_validate_valid_tools_markdown")


def test_validate_valid_resources_markdown():
    """Test validation of valid resources markdown."""
    validator = MarkdownValidator()
    content = create_valid_resources_markdown()

    result = validator.validate_markdown(content)

    assert result.is_valid == True
    assert len(result.errors) == 0
    assert result.content_stats['resources_count'] == 2
    assert result.content_stats['details_blocks'] == 1

    print("✓ test_validate_valid_resources_markdown")


def test_validate_invalid_markdown():
    """Test validation of invalid markdown."""
    validator = MarkdownValidator()
    content = create_invalid_markdown()

    result = validator.validate_markdown(content)

    assert result.is_valid == False
    assert len(result.errors) > 0

    # Check for specific error types
    error_text = ' '.join(result.errors)
    assert "Unbalanced" in error_text or "Malformed" in error_text

    print("✓ test_validate_invalid_markdown")


def test_validate_empty_content():
    """Test validation of empty content."""
    validator = MarkdownValidator()

    result = validator.validate_markdown("")

    assert result.is_valid == False
    assert any("empty" in error.lower() for error in result.errors)

    print("✓ test_validate_empty_content")


def test_validate_tools_section_specific():
    """Test tools section specific validation."""
    validator = MarkdownValidator()

    # Valid tools section (allow some missing categories since we're testing structure)
    valid_content = create_valid_tools_markdown()
    errors = validator.validate_tools_section(valid_content)
    # Should have some missing category errors but no structural errors
    structural_errors = [e for e in errors if "missing" not in e.lower()]
    assert len(structural_errors) == 0

    # Invalid tools section (missing details)
    invalid_content = "Just some text without proper structure"
    errors = validator.validate_tools_section(invalid_content)
    assert len(errors) > 0
    assert any("missing" in error.lower() for error in errors)

    print("✓ test_validate_tools_section_specific")


def test_validate_resources_section_specific():
    """Test resources section specific validation."""
    validator = MarkdownValidator()

    # Valid resources section
    valid_content = create_valid_resources_markdown()
    errors = validator.validate_resources_section(valid_content)
    assert len(errors) == 0

    # Invalid resources section
    invalid_content = "Some text without resources"
    errors = validator.validate_resources_section(invalid_content)
    assert len(errors) > 0

    print("✓ test_validate_resources_section_specific")


def test_html_tag_validation():
    """Test HTML tag balance validation."""
    validator = MarkdownValidator()

    # Unbalanced details tags
    unbalanced_content = "<details><summary>Test</summary><!-- Missing closing details -->"
    result = validator.validate_markdown(unbalanced_content)

    assert result.is_valid == False
    assert any("Unbalanced <details>" in error for error in result.errors)

    # Unbalanced summary tags
    unbalanced_summary = "<details><summary>Test<!-- Missing closing summary --></details>"
    result = validator.validate_markdown(unbalanced_summary)

    assert result.is_valid == False
    assert any("Unbalanced <summary>" in error for error in result.errors)

    print("✓ test_html_tag_validation")


def test_markdown_syntax_validation():
    """Test markdown syntax validation."""
    validator = MarkdownValidator()

    # Malformed list items
    malformed_lists = """
- Good list item
  -Bad list item (missing space)
  - Good nested item
"""
    result = validator.validate_markdown(malformed_lists)

    assert any("Malformed list item" in error for error in result.errors)

    print("✓ test_markdown_syntax_validation")


def test_content_quality_warnings():
    """Test content quality validation warnings."""
    validator = MarkdownValidator()

    # Short descriptions
    short_desc_content = """
- **memory.test**
  - Description: Short
  - Title: Test Tool
"""
    result = validator.validate_markdown(short_desc_content)

    assert any("Very short description" in warning for warning in result.warnings)

    # Missing periods
    no_period_content = """
- **memory.test**
  - Description: This description has no period
  - Title: Test Tool
"""
    result = validator.validate_markdown(no_period_content)

    assert any("missing period" in warning.lower() for warning in result.warnings)

    print("✓ test_content_quality_warnings")


def test_formatting_consistency_warnings():
    """Test formatting consistency validation."""
    validator = MarkdownValidator()

    # Inconsistent indentation
    inconsistent_indent = """
- Item 1
   - Odd indentation (3 spaces)
  - Good indentation (2 spaces)
"""
    result = validator.validate_markdown(inconsistent_indent)

    assert any("Inconsistent list indentation" in warning for warning in result.warnings)

    print("✓ test_formatting_consistency_warnings")


def test_content_stats_calculation():
    """Test content statistics calculation."""
    validator = MarkdownValidator()
    content = create_valid_tools_markdown()

    result = validator.validate_markdown(content)
    stats = result.content_stats

    # Verify expected statistics
    assert stats['tools_count'] == 2
    assert stats['resources_count'] == 0
    assert stats['details_blocks'] == 2
    assert stats['generation_comments'] == 2
    assert stats['total_lines'] > 0
    assert stats['list_items'] > 0

    print("✓ test_content_stats_calculation")


def test_section_validator_markers():
    """Test SectionValidator marker validation."""
    start_marker = "<!-- START TOOLS -->"
    end_marker = "<!-- END TOOLS -->"

    # Valid markers
    valid_content = f"Before\n{start_marker}\nContent\n{end_marker}\nAfter"
    errors = SectionValidator.validate_section_markers(valid_content, start_marker, end_marker)
    assert len(errors) == 0

    # Missing markers
    no_markers = "Content without markers"
    errors = SectionValidator.validate_section_markers(no_markers, start_marker, end_marker)
    assert len(errors) > 0
    assert any("not found" in error for error in errors)

    # Unbalanced markers
    unbalanced = f"Before\n{start_marker}\nContent\n{start_marker}\nAfter"
    errors = SectionValidator.validate_section_markers(unbalanced, start_marker, end_marker)
    assert len(errors) > 0
    assert any("Unbalanced" in error for error in errors)

    print("✓ test_section_validator_markers")


def test_section_validator_extract_content():
    """Test SectionValidator content extraction."""
    start_marker = "<!-- START -->"
    end_marker = "<!-- END -->"
    content = f"Before\n{start_marker}\nExtracted Content\n{end_marker}\nAfter"

    extracted = SectionValidator.extract_section_content(content, start_marker, end_marker)
    assert extracted == "Extracted Content"

    # No markers
    no_markers = "Content without markers"
    extracted = SectionValidator.extract_section_content(no_markers, start_marker, end_marker)
    assert extracted == ""

    print("✓ test_section_validator_extract_content")


def test_section_validator_structure():
    """Test SectionValidator structure validation."""
    content = create_valid_tools_markdown()
    expected_elements = ["<details>", "<summary>", "Entity Management"]

    errors = SectionValidator.validate_section_content_structure(content, expected_elements)
    assert len(errors) == 0

    # Missing elements
    missing_elements = ["<details>", "Missing Element"]
    errors = SectionValidator.validate_section_content_structure(content, missing_elements)
    assert len(errors) == 1
    assert "Missing Element" in errors[0]

    print("✓ test_section_validator_structure")


def test_content_enhancer_fix_common_issues():
    """Test ContentEnhancer common issue fixes."""
    # Content with trailing whitespace and excessive blank lines
    problematic_content = """Line with trailing spaces   \n\n\n\nLine after many blanks\n\n\n"""

    fixed_content, changes = ContentEnhancer.fix_common_issues(problematic_content)

    assert len(changes) > 0
    assert any("trailing whitespace" in change.lower() for change in changes)
    assert any("blank lines" in change.lower() for change in changes)

    # Should have single trailing newline
    assert fixed_content.endswith('\n')
    assert not fixed_content.endswith('\n\n')

    print("✓ test_content_enhancer_fix_common_issues")


def test_content_enhancer_list_spacing():
    """Test ContentEnhancer list spacing fixes."""
    bad_list = """- Good item\n  -Bad item\n  - Good nested item"""

    fixed_content, changes = ContentEnhancer.fix_common_issues(bad_list)

    assert "Fixed list item spacing" in changes
    assert "  - Bad item" in fixed_content  # Should be fixed

    print("✓ test_content_enhancer_list_spacing")


def test_content_enhancer_enhance_readability():
    """Test ContentEnhancer readability enhancements."""
    # Content with missing periods and inconsistent capitalization
    content_to_enhance = """
- Title: test tool name
  - Description: This is a description without period
"""

    enhanced_content, changes = ContentEnhancer.enhance_readability(content_to_enhance)

    assert len(changes) > 0
    assert any("periods" in change.lower() for change in changes)
    assert "period." in enhanced_content

    print("✓ test_content_enhancer_enhance_readability")


def test_validation_result_summary():
    """Test ValidationResult summary generation."""
    # Create a validation result with errors and warnings
    result = ValidationResult(
        is_valid=False,
        errors=["Error 1", "Error 2"],
        warnings=["Warning 1"],
        content_stats={'tools_count': 2, 'total_lines': 10}
    )

    summary = result.get_summary()

    assert "✗ Validation failed" in summary
    assert "Errors: 2" in summary
    assert "Warnings: 1" in summary
    assert "Tools Count: 2" in summary

    print("✓ test_validation_result_summary")


def test_validation_result_detailed_report():
    """Test ValidationResult detailed report generation."""
    result = ValidationResult(
        is_valid=True,
        errors=[],
        warnings=["Minor warning"],
        content_stats={'tools_count': 1}
    )

    report = result.get_detailed_report()

    assert "=== Markdown Validation Report ===" in report
    assert "✓ Overall Status: VALID" in report
    assert "WARNINGS (1):" in report
    assert "CONTENT STATISTICS:" in report

    print("✓ test_validation_result_detailed_report")


def test_validator_integration_with_generators():
    """Test validator integration with generated content."""
    validator = MarkdownValidator()

    # Test both tools and resources together
    combined_content = create_valid_tools_markdown() + "\n\n" + create_valid_resources_markdown()

    result = validator.validate_markdown(combined_content)

    assert result.is_valid == True
    assert result.content_stats['tools_count'] == 2
    assert result.content_stats['resources_count'] == 2
    assert result.content_stats['details_blocks'] == 3
    assert result.content_stats['generation_comments'] == 3

    print("✓ test_validator_integration_with_generators")


def run_all_tests():
    """Run all test functions."""
    test_functions = [
        test_markdown_validator_initialization,
        test_validate_valid_tools_markdown,
        test_validate_valid_resources_markdown,
        test_validate_invalid_markdown,
        test_validate_empty_content,
        test_validate_tools_section_specific,
        test_validate_resources_section_specific,
        test_html_tag_validation,
        test_markdown_syntax_validation,
        test_content_quality_warnings,
        test_formatting_consistency_warnings,
        test_content_stats_calculation,
        test_section_validator_markers,
        test_section_validator_extract_content,
        test_section_validator_structure,
        test_content_enhancer_fix_common_issues,
        test_content_enhancer_list_spacing,
        test_content_enhancer_enhance_readability,
        test_validation_result_summary,
        test_validation_result_detailed_report,
        test_validator_integration_with_generators,
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
