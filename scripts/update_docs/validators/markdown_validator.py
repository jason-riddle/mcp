"""
Markdown structure and content validator.
"""

import re
from typing import Dict, List, Tuple
from pathlib import Path


class MarkdownValidator:
    """Validate markdown content structure and format."""

    def __init__(self):
        """Initialize markdown validator."""
        pass

    def validate_markdown(self, content: str) -> 'ValidationResult':
        """Validate complete markdown content and return detailed results."""
        errors = []
        warnings = []

        # Run all validation checks
        errors.extend(self._validate_structure(content))
        errors.extend(self._validate_html_tags(content))
        errors.extend(self._validate_markdown_syntax(content))
        warnings.extend(self._validate_content_quality(content))
        warnings.extend(self._validate_formatting_consistency(content))

        return ValidationResult(
            is_valid=len(errors) == 0,
            errors=errors,
            warnings=warnings,
            content_stats=self._calculate_content_stats(content)
        )

    def validate_tools_section(self, content: str) -> List[str]:
        """Validate tools section specific requirements."""
        errors = []

        # Check for required tools section elements
        if not re.search(r'<details>', content, re.IGNORECASE):
            errors.append("Tools section missing collapsible details elements")

        if not re.search(r'<summary><b>', content, re.IGNORECASE):
            errors.append("Tools section missing summary headers")

        # Check for expected tool categories
        expected_categories = [
            'Entity Management',
            'Relationship Management',
            'Graph Operations',
            'Observation Management'
        ]

        for category in expected_categories:
            if category not in content:
                errors.append(f"Missing expected tool category: {category}")

        # Check for generation comment
        if not re.search(r'<!-- NOTE: This has been generated.*?--', content):
            errors.append("Missing generation comment in tools section")

        # Validate tool entries have required fields
        tool_pattern = r'\*\*memory\.[a-z_]+\*\*'
        tools = re.findall(tool_pattern, content)
        if not tools:
            errors.append("No valid tool entries found")

        return errors

    def validate_resources_section(self, content: str) -> List[str]:
        """Validate resources section specific requirements."""
        errors = []

        # Check for required resources section elements
        if not re.search(r'<details>', content, re.IGNORECASE):
            errors.append("Resources section missing collapsible details elements")

        if "Memory Resources" not in content:
            errors.append("Resources section missing 'Memory Resources' header")

        # Check for generation comment
        if not re.search(r'<!-- NOTE: This has been generated.*?--', content):
            errors.append("Missing generation comment in resources section")

        # Validate resource entries
        resource_pattern = r'\*\*memory://[a-z_]+\*\*'
        resources = re.findall(resource_pattern, content)
        if not resources:
            errors.append("No valid resource entries found")

        # Check for expected resource structure
        if "- Title:" not in content:
            errors.append("Resources missing title fields")

        if "- Description:" not in content:
            errors.append("Resources missing description fields")

        return errors

    def _validate_structure(self, content: str) -> List[str]:
        """Validate overall markdown structure."""
        errors = []

        # Check for empty content
        if not content.strip():
            errors.append("Content is empty")
            return errors

        # Check for basic markdown structure
        lines = content.split('\n')
        if len(lines) < 3:
            errors.append("Content too short to be valid markdown")

        # Check for proper line endings
        if content.endswith('\n\n\n'):
            errors.append("Content has excessive trailing newlines")

        return errors

    def _validate_html_tags(self, content: str) -> List[str]:
        """Validate HTML tag balance and syntax."""
        errors = []

        # Check details/summary tag balance
        details_open = len(re.findall(r'<details>', content, re.IGNORECASE))
        details_close = len(re.findall(r'</details>', content, re.IGNORECASE))
        if details_open != details_close:
            errors.append(f"Unbalanced <details> tags: {details_open} open, {details_close} close")

        # Check summary tag balance
        summary_open = len(re.findall(r'<summary>', content, re.IGNORECASE))
        summary_close = len(re.findall(r'</summary>', content, re.IGNORECASE))
        if summary_open != summary_close:
            errors.append(f"Unbalanced <summary> tags: {summary_open} open, {summary_close} close")

        # Check for malformed HTML tags
        malformed_tags = re.findall(r'<[^>]*[^>]$', content, re.MULTILINE)
        if malformed_tags:
            errors.append(f"Malformed HTML tags found: {malformed_tags[:3]}")

        # Check for unclosed bold tags (more precise pattern)
        # Look for ** that isn't followed by another ** on the same line
        lines = content.split('\n')
        for i, line in enumerate(lines, 1):
            # Count ** pairs in the line
            bold_count = line.count('**')
            if bold_count % 2 != 0:
                errors.append(f"Line {i}: Unclosed bold markdown found")

        return errors

    def _validate_markdown_syntax(self, content: str) -> List[str]:
        """Validate markdown syntax correctness."""
        errors = []

        # Check for malformed lists
        lines = content.split('\n')
        for i, line in enumerate(lines, 1):
            stripped = line.strip()

            # Check list indentation consistency
            if re.match(r'^  -', line) and not re.match(r'^  - ', line):
                errors.append(f"Line {i}: Malformed list item (missing space after dash)")

            # Check for incomplete markdown links
            if '[' in stripped and ']' in stripped and not re.search(r'\[[^\]]*\]\([^)]*\)', stripped):
                if not re.search(r'\[[^\]]*\]:', stripped):  # Not a reference link
                    errors.append(f"Line {i}: Incomplete markdown link syntax")

        return errors

    def _validate_content_quality(self, content: str) -> List[str]:
        """Validate content quality and provide warnings."""
        warnings = []

        # Check for very short descriptions
        description_pattern = r'Description: ([^\n]*)'
        descriptions = re.findall(description_pattern, content)
        for desc in descriptions:
            if len(desc.strip()) < 10:
                warnings.append(f"Very short description found: '{desc.strip()}'")

        # Check for missing periods in descriptions
        for desc in descriptions:
            if desc.strip() and not desc.strip().endswith('.'):
                warnings.append(f"Description missing period: '{desc.strip()}'")

        # Check for duplicate content
        lines = [line.strip() for line in content.split('\n') if line.strip()]
        unique_lines = set(lines)
        if len(lines) != len(unique_lines):
            duplicate_count = len(lines) - len(unique_lines)
            warnings.append(f"Found {duplicate_count} duplicate lines")

        return warnings

    def _validate_formatting_consistency(self, content: str) -> List[str]:
        """Validate formatting consistency throughout the content."""
        warnings = []

        # Check for consistent indentation in lists
        list_lines = [line for line in content.split('\n') if re.match(r'^\s*-', line)]
        indentations = [len(line) - len(line.lstrip()) for line in list_lines]

        if indentations:
            # Check for consistent 2-space indentation for nested items
            nested_items = [indent for indent in indentations if indent > 0]
            if nested_items and not all(indent % 2 == 0 for indent in nested_items):
                warnings.append("Inconsistent list indentation (should use 2-space increments)")

        # Check for consistent bold formatting
        bold_patterns = re.findall(r'\*\*[^*]+\*\*', content)
        if bold_patterns:
            # Check for mixed bold styles (** vs __)
            underscore_bold = re.findall(r'__[^_]+__', content)
            if underscore_bold:
                warnings.append("Mixed bold formatting styles found (** and __)")

        return warnings

    def _calculate_content_stats(self, content: str) -> Dict[str, int]:
        """Calculate content statistics."""
        lines = content.split('\n')

        return {
            'total_lines': len(lines),
            'non_empty_lines': len([line for line in lines if line.strip()]),
            'tools_count': len(re.findall(r'\*\*memory\.[a-z_]+\*\*', content)),
            'resources_count': len(re.findall(r'\*\*memory://[a-z_]+\*\*', content)),
            'details_blocks': len(re.findall(r'<details>', content, re.IGNORECASE)),
            'list_items': len(re.findall(r'^\s*-', content, re.MULTILINE)),
            'generation_comments': len(re.findall(r'<!-- NOTE: This has been generated.*?--', content))
        }


class SectionValidator:
    """Helper class for validating specific markdown sections."""

    @staticmethod
    def validate_section_markers(content: str, start_marker: str, end_marker: str) -> List[str]:
        """Validate that section markers are properly placed and balanced."""
        errors = []

        start_count = content.count(start_marker)
        end_count = content.count(end_marker)

        if start_count == 0 and end_count == 0:
            errors.append(f"Section markers not found: {start_marker} / {end_marker}")
        elif start_count != end_count:
            errors.append(f"Unbalanced section markers: {start_count} start, {end_count} end")
        elif start_count > 1:
            errors.append(f"Multiple section markers found: {start_count} instances")

        # Check marker order
        if start_count == 1 and end_count == 1:
            start_pos = content.find(start_marker)
            end_pos = content.find(end_marker)
            if start_pos > end_pos:
                errors.append("Section end marker appears before start marker")

        return errors

    @staticmethod
    def extract_section_content(content: str, start_marker: str, end_marker: str) -> str:
        """Extract content between section markers."""
        start_pos = content.find(start_marker)
        end_pos = content.find(end_marker)

        if start_pos == -1 or end_pos == -1:
            return ""

        # Extract content between markers
        section_start = start_pos + len(start_marker)
        return content[section_start:end_pos].strip()

    @staticmethod
    def validate_section_content_structure(content: str, expected_elements: List[str]) -> List[str]:
        """Validate that section content contains expected structural elements."""
        errors = []

        for element in expected_elements:
            if element not in content:
                errors.append(f"Section missing expected element: {element}")

        return errors


class ContentEnhancer:
    """Helper class for enhancing markdown content with improvements."""

    @staticmethod
    def fix_common_issues(content: str) -> Tuple[str, List[str]]:
        """Fix common markdown issues and return fixed content with change log."""
        changes = []
        fixed_content = content

        # Fix trailing whitespace
        original_lines = fixed_content.split('\n')
        fixed_lines = [line.rstrip() for line in original_lines]
        if original_lines != fixed_lines:
            changes.append("Removed trailing whitespace from lines")
            fixed_content = '\n'.join(fixed_lines)

        # Fix excessive blank lines
        while '\n\n\n' in fixed_content:
            fixed_content = fixed_content.replace('\n\n\n', '\n\n')
            if "Reduced excessive blank lines" not in changes:
                changes.append("Reduced excessive blank lines")

        # Ensure single trailing newline
        if not fixed_content.endswith('\n'):
            fixed_content += '\n'
            changes.append("Added trailing newline")
        elif fixed_content.endswith('\n\n'):
            fixed_content = fixed_content.rstrip('\n') + '\n'
            changes.append("Fixed trailing newlines")

        # Fix list spacing (ensure space after dash)
        lines = fixed_content.split('\n')
        for i, line in enumerate(lines):
            if re.match(r'^(\s*)-([^ ])', line):
                lines[i] = re.sub(r'^(\s*)-([^ ])', r'\1- \2', line)
                if "Fixed list item spacing" not in changes:
                    changes.append("Fixed list item spacing")

        if changes and "Fixed list item spacing" in changes:
            fixed_content = '\n'.join(lines)

        return fixed_content, changes

    @staticmethod
    def enhance_readability(content: str) -> Tuple[str, List[str]]:
        """Enhance content readability and return enhanced content with change log."""
        changes = []
        enhanced_content = content

        # Ensure descriptions end with periods
        def add_period_to_description(match):
            desc = match.group(1).strip()
            if desc and not desc.endswith('.'):
                return f"Description: {desc}."
            return match.group(0)

        original_content = enhanced_content
        enhanced_content = re.sub(r'Description: ([^\n]*)', add_period_to_description, enhanced_content)
        if enhanced_content != original_content:
            changes.append("Added periods to descriptions")

        # Ensure consistent capitalization in titles
        def capitalize_title(match):
            title = match.group(1)
            # Capitalize first letter of each word except common articles
            words = title.split()
            capitalized_words = []
            for i, word in enumerate(words):
                if i == 0 or word.lower() not in ['a', 'an', 'the', 'of', 'in', 'on', 'at', 'to', 'for', 'and', 'or', 'but']:
                    capitalized_words.append(word.capitalize())
                else:
                    capitalized_words.append(word.lower())
            return f"Title: {' '.join(capitalized_words)}"

        original_content = enhanced_content
        enhanced_content = re.sub(r'Title: ([^\n]*)', capitalize_title, enhanced_content)
        if enhanced_content != original_content:
            changes.append("Standardized title capitalization")

        return enhanced_content, changes


class ValidationResult:
    """Container for validation results."""

    def __init__(self, is_valid: bool, errors: List[str], warnings: List[str], content_stats: Dict[str, int]):
        """Initialize validation result."""
        self.is_valid = is_valid
        self.errors = errors
        self.warnings = warnings
        self.content_stats = content_stats

    def get_summary(self) -> str:
        """Get a summary of validation results."""
        summary_lines = []

        if self.is_valid:
            summary_lines.append("✓ Validation passed")
        else:
            summary_lines.append("✗ Validation failed")

        if self.errors:
            summary_lines.append(f"Errors: {len(self.errors)}")
            for error in self.errors[:3]:  # Show first 3 errors
                summary_lines.append(f"  - {error}")
            if len(self.errors) > 3:
                summary_lines.append(f"  ... and {len(self.errors) - 3} more")

        if self.warnings:
            summary_lines.append(f"Warnings: {len(self.warnings)}")
            for warning in self.warnings[:3]:  # Show first 3 warnings
                summary_lines.append(f"  - {warning}")
            if len(self.warnings) > 3:
                summary_lines.append(f"  ... and {len(self.warnings) - 3} more")

        # Add content stats
        summary_lines.append("Content Statistics:")
        for key, value in self.content_stats.items():
            formatted_key = key.replace('_', ' ').title()
            summary_lines.append(f"  {formatted_key}: {value}")

        return '\n'.join(summary_lines)

    def get_detailed_report(self) -> str:
        """Get a detailed validation report."""
        report_lines = ["=== Markdown Validation Report ===", ""]

        # Overall status
        if self.is_valid:
            report_lines.append("✓ Overall Status: VALID")
        else:
            report_lines.append("✗ Overall Status: INVALID")
        report_lines.append("")

        # Errors section
        if self.errors:
            report_lines.append(f"ERRORS ({len(self.errors)}):")
            for i, error in enumerate(self.errors, 1):
                report_lines.append(f"  {i}. {error}")
            report_lines.append("")

        # Warnings section
        if self.warnings:
            report_lines.append(f"WARNINGS ({len(self.warnings)}):")
            for i, warning in enumerate(self.warnings, 1):
                report_lines.append(f"  {i}. {warning}")
            report_lines.append("")

        # Statistics section
        report_lines.append("CONTENT STATISTICS:")
        for key, value in self.content_stats.items():
            formatted_key = key.replace('_', ' ').title()
            report_lines.append(f"  {formatted_key}: {value}")

        return '\n'.join(report_lines)
