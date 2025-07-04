"""
Tools section markdown generator.
"""

from typing import Dict, List
from ..config import DocumentationConfig


class ToolsGenerator:
    """Generate formatted markdown content for tools section."""

    def __init__(self, config: DocumentationConfig):
        """Initialize tools generator with configuration."""
        self.config = config

    def generate_section(self, tools: List[Dict]) -> str:
        """Generate complete tools section markdown."""
        if not tools:
            return "No tools found."

        lines = []

        # Group tools by category
        categorized_tools = self._categorize_tools(tools)

        # Generate each category
        for category, category_tools in categorized_tools.items():
            if not category_tools:
                continue

            lines.extend(self._generate_category_section(category, category_tools))

        return '\n'.join(lines)

    def _categorize_tools(self, tools: List[Dict]) -> Dict[str, List[Dict]]:
        """Categorize tools based on configuration."""
        categorized = {}

        # Initialize categories from config
        for category in self.config.tool_categories.keys():
            categorized[category] = []

        # Add 'Other' category for uncategorized tools
        categorized['Other'] = []

        # Categorize each tool
        for tool in tools:
            category = self.config.categorize_tool(tool['name'])
            if category in categorized:
                categorized[category].append(tool)
            else:
                categorized['Other'].append(tool)

        # Remove empty categories
        return {cat: tools for cat, tools in categorized.items() if tools}

    def _generate_category_section(self, category: str, tools: List[Dict]) -> List[str]:
        """Generate markdown for a single category of tools."""
        lines = [
            "<details>",
            f"<summary><b>{category}</b></summary>",
            ""
        ]

        for tool in tools:
            lines.extend(self._format_tool(tool))

        lines.extend(["</details>", ""])

        return lines

    def _format_tool(self, tool: Dict) -> List[str]:
        """Format a single tool for documentation."""
        lines = [
            "<!-- NOTE: This has been generated via update-docs.py --->",
            "",
            f"- **{tool['name']}**",
            f"  - Title: {tool['title']}",
            f"  - Description: {tool['description']}"
        ]

        # Add parameters section
        if tool.get('parameters'):
            lines.append("  - Parameters:")
            for param in tool['parameters']:
                required_text = "" if param.get('required', True) else ", optional"
                lines.append(f"    - `{param['name']}` ({param['type']}{required_text}): {param['description']}")
        else:
            lines.append("  - Parameters: None")

        # Add read-only indicator
        read_only_str = str(tool.get('read_only', False)).lower()
        lines.append(f"  - Read-only: **{read_only_str}**")
        lines.append("")

        return lines


class ToolsFormatter:
    """Helper class for formatting tool information."""

    @staticmethod
    def format_parameter(param: Dict) -> str:
        """Format a single parameter for documentation."""
        required_text = "" if param.get('required', True) else ", optional"
        return f"`{param['name']}` ({param['type']}{required_text}): {param['description']}"

    @staticmethod
    def format_tool_header(tool: Dict) -> str:
        """Format tool header with name and title."""
        return f"**{tool['name']}** - {tool['title']}"

    @staticmethod
    def format_read_only_indicator(is_read_only: bool) -> str:
        """Format read-only indicator."""
        return f"**{str(is_read_only).lower()}**"

    @staticmethod
    def escape_markdown(text: str) -> str:
        """Escape special markdown characters in text."""
        # Escape common markdown characters
        escapes = {
            '*': r'\*',
            '_': r'\_',
            '`': r'\`',
            '[': r'\[',
            ']': r'\]',
            '(': r'\(',
            ')': r'\)',
            '#': r'\#',
            '+': r'\+',
            '-': r'\-',
            '.': r'\.',
            '!': r'\!'
        }

        for char, escape in escapes.items():
            text = text.replace(char, escape)

        return text


class ToolsSorter:
    """Helper class for sorting tools in different ways."""

    @staticmethod
    def sort_by_name(tools: List[Dict]) -> List[Dict]:
        """Sort tools alphabetically by name."""
        return sorted(tools, key=lambda t: t['name'])

    @staticmethod
    def sort_by_title(tools: List[Dict]) -> List[Dict]:
        """Sort tools alphabetically by title."""
        return sorted(tools, key=lambda t: t['title'])

    @staticmethod
    def sort_by_read_only(tools: List[Dict]) -> List[Dict]:
        """Sort tools with read-only tools last."""
        return sorted(tools, key=lambda t: (t.get('read_only', False), t['name']))

    @staticmethod
    def sort_by_parameter_count(tools: List[Dict]) -> List[Dict]:
        """Sort tools by number of parameters (ascending)."""
        return sorted(tools, key=lambda t: len(t.get('parameters', [])))


class ToolsValidator:
    """Helper class for validating tool data before generation."""

    @staticmethod
    def validate_tool(tool: Dict) -> List[str]:
        """Validate a single tool and return list of validation errors."""
        errors = []

        # Required fields
        required_fields = ['name', 'title', 'description']
        for field in required_fields:
            if field not in tool or not tool[field]:
                errors.append(f"Missing or empty required field: {field}")

        # Validate name format
        if 'name' in tool and not tool['name'].startswith('memory.'):
            errors.append(f"Tool name should start with 'memory.': {tool['name']}")

        # Validate parameters if present
        if 'parameters' in tool and tool['parameters']:
            for i, param in enumerate(tool['parameters']):
                param_errors = ToolsValidator.validate_parameter(param, i)
                errors.extend(param_errors)

        return errors

    @staticmethod
    def validate_parameter(param: Dict, index: int) -> List[str]:
        """Validate a single parameter."""
        errors = []

        # Required parameter fields
        required_fields = ['name', 'type', 'description']
        for field in required_fields:
            if field not in param or not param[field]:
                errors.append(f"Parameter {index}: Missing or empty required field: {field}")

        # Validate parameter name format
        if 'name' in param and not param['name'].replace('_', '').replace('-', '').isalnum():
            errors.append(f"Parameter {index}: Invalid name format: {param['name']}")

        return errors

    @staticmethod
    def validate_tools_list(tools: List[Dict]) -> List[str]:
        """Validate entire tools list."""
        errors = []

        if not tools:
            errors.append("Tools list is empty")
            return errors

        # Check for duplicate tool names
        names = [tool.get('name', '') for tool in tools]
        duplicates = set([name for name in names if names.count(name) > 1])
        if duplicates:
            errors.append(f"Duplicate tool names found: {', '.join(duplicates)}")

        # Validate each tool
        for i, tool in enumerate(tools):
            tool_errors = ToolsValidator.validate_tool(tool)
            errors.extend([f"Tool {i}: {error}" for error in tool_errors])

        return errors
