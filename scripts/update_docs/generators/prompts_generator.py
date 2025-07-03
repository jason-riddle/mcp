"""
Prompts section markdown generator for MCP prompts documentation.
"""

from typing import Dict, List
from ..config import DocumentationConfig


class PromptsGenerator:
    """Generate formatted markdown content for prompts section."""

    def __init__(self, config: DocumentationConfig):
        """Initialize prompts generator with configuration."""
        self.config = config

    def generate_section(self, prompts: List[Dict]) -> str:
        """Generate complete prompts section markdown."""
        if not prompts:
            return "No prompts found."

        lines = []

        # Group prompts by category
        categorized_prompts = self._categorize_prompts(prompts)

        # Generate each category
        for category, category_prompts in categorized_prompts.items():
            if not category_prompts:
                continue

            lines.extend(self._generate_category_section(category, category_prompts))

        return '\n'.join(lines)

    def _categorize_prompts(self, prompts: List[Dict]) -> Dict[str, List[Dict]]:
        """Categorize prompts based on configuration or naming patterns."""
        categorized = {
            'Memory Guidance': [],
            'Graph Operations': [],
            'Data Analysis': [],
            'Other': []
        }

        for prompt in prompts:
            category = self._determine_prompt_category(prompt)
            if category in categorized:
                categorized[category].append(prompt)
            else:
                categorized['Other'].append(prompt)

        # Remove empty categories
        return {cat: prompts for cat, prompts in categorized.items() if prompts}

    def _determine_prompt_category(self, prompt: Dict) -> str:
        """Determine category for a prompt based on name and description."""
        name = prompt.get('name', '').lower()
        description = prompt.get('description', '').lower()

        if 'memory' in name or 'guidance' in description:
            return 'Memory Guidance'
        elif 'graph' in name or 'entity' in description or 'relation' in description:
            return 'Graph Operations'
        elif 'analysis' in name or 'analyze' in description or 'search' in description:
            return 'Data Analysis'
        else:
            return 'Other'

    def _generate_category_section(self, category: str, prompts: List[Dict]) -> List[str]:
        """Generate markdown for a single category of prompts."""
        lines = [
            "<details>",
            f"<summary><b>{category}</b></summary>",
            ""
        ]

        for prompt in prompts:
            lines.extend(self._format_prompt(prompt))

        lines.extend(["</details>", ""])

        return lines

    def _format_prompt(self, prompt: Dict) -> List[str]:
        """Format a single prompt for documentation."""
        lines = [
            "<!-- NOTE: This has been generated via update-docs.py --->",
            "",
            f"- **{prompt['name']}**",
            f"  - Title: {prompt.get('title', 'No title')}"
        ]

        # Add description
        description = prompt.get('description', '')
        if description:
            lines.append(f"  - Description: {PromptsFormatter.clean_description(description)}")
        else:
            lines.append("  - Description: No description available.")

        # Add arguments if present
        if prompt.get('arguments'):
            lines.append("  - Arguments:")
            for arg in prompt['arguments']:
                arg_text = PromptsFormatter.format_argument(arg)
                lines.append(f"    - {arg_text}")
        else:
            lines.append("  - Arguments: None")

        lines.append("")

        return lines


class PromptsFormatter:
    """Helper class for formatting prompt information."""

    @staticmethod
    def format_argument(arg: Dict) -> str:
        """Format a single argument for documentation."""
        name = arg.get('name', 'unnamed')
        arg_type = arg.get('type', 'unknown')
        description = arg.get('description', 'No description')
        required = arg.get('required', True)

        required_text = "" if required else ", optional"
        return f"`{name}` ({arg_type}{required_text}): {description}"

    @staticmethod
    def format_prompt_header(prompt: Dict) -> str:
        """Format prompt header with name and title."""
        return f"**{prompt['name']}** - {prompt.get('title', 'No title')}"

    @staticmethod
    def clean_description(description: str) -> str:
        """Clean and format prompt description."""
        if not description:
            return "No description available."

        # Remove extra whitespace and normalize
        cleaned = ' '.join(description.split())

        # Ensure description ends with period
        if not cleaned.endswith('.'):
            cleaned += '.'

        return cleaned

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


class PromptsSorter:
    """Helper class for sorting prompts in different ways."""

    @staticmethod
    def sort_by_name(prompts: List[Dict]) -> List[Dict]:
        """Sort prompts alphabetically by name."""
        return sorted(prompts, key=lambda p: p.get('name', ''))

    @staticmethod
    def sort_by_title(prompts: List[Dict]) -> List[Dict]:
        """Sort prompts alphabetically by title."""
        return sorted(prompts, key=lambda p: p.get('title', ''))

    @staticmethod
    def sort_by_argument_count(prompts: List[Dict]) -> List[Dict]:
        """Sort prompts by number of arguments (ascending)."""
        return sorted(prompts, key=lambda p: len(p.get('arguments', [])))


class PromptsValidator:
    """Helper class for validating prompt data before generation."""

    @staticmethod
    def validate_prompt(prompt: Dict) -> List[str]:
        """Validate a single prompt and return list of validation errors."""
        errors = []

        # Required fields
        required_fields = ['name']
        for field in required_fields:
            if field not in prompt or not prompt[field]:
                errors.append(f"Missing or empty required field: {field}")

        # Validate name format
        if 'name' in prompt:
            name = prompt['name']
            if not isinstance(name, str) or not name.strip():
                errors.append("Prompt name must be a non-empty string")

        # Validate arguments if present
        if 'arguments' in prompt and prompt['arguments']:
            for i, arg in enumerate(prompt['arguments']):
                arg_errors = PromptsValidator.validate_argument(arg, i)
                errors.extend(arg_errors)

        return errors

    @staticmethod
    def validate_argument(arg: Dict, index: int) -> List[str]:
        """Validate a single argument."""
        errors = []

        # Required argument fields
        required_fields = ['name']
        for field in required_fields:
            if field not in arg or not arg[field]:
                errors.append(f"Argument {index}: Missing or empty required field: {field}")

        # Validate argument name format
        if 'name' in arg:
            name = arg['name']
            if not isinstance(name, str) or not name.replace('_', '').replace('-', '').isalnum():
                errors.append(f"Argument {index}: Invalid name format: {name}")

        return errors

    @staticmethod
    def validate_prompts_list(prompts: List[Dict]) -> List[str]:
        """Validate entire prompts list."""
        errors = []

        if not prompts:
            errors.append("Prompts list is empty")
            return errors

        # Check for duplicate prompt names
        names = [prompt.get('name', '') for prompt in prompts]
        duplicates = set([name for name in names if names.count(name) > 1])
        if duplicates:
            errors.append(f"Duplicate prompt names found: {', '.join(duplicates)}")

        # Validate each prompt
        for i, prompt in enumerate(prompts):
            prompt_errors = PromptsValidator.validate_prompt(prompt)
            errors.extend([f"Prompt {i}: {error}" for error in prompt_errors])

        return errors


class PromptsEnhancer:
    """Helper class for enhancing prompt data with additional information."""

    @staticmethod
    def enhance_descriptions(prompts: List[Dict]) -> List[Dict]:
        """Enhance prompt descriptions with better defaults."""
        enhanced = []

        for prompt in prompts:
            enhanced_prompt = prompt.copy()

            # Enhance description if empty or generic
            description = prompt.get('description', '')
            if not description or description in ['', 'No description available.']:
                enhanced_prompt['description'] = PromptsEnhancer._generate_description(prompt)
            else:
                enhanced_prompt['description'] = PromptsFormatter.clean_description(description)

            enhanced.append(enhanced_prompt)

        return enhanced

    @staticmethod
    def _generate_description(prompt: Dict) -> str:
        """Generate a description based on prompt name and arguments."""
        name = prompt.get('name', '')
        args = prompt.get('arguments', [])

        if 'memory' in name.lower():
            return "Provides guidance for working with the memory graph system."
        elif 'graph' in name.lower():
            return "Assists with graph operations and data manipulation."
        elif 'search' in name.lower():
            return "Helps with searching and analyzing data within the system."
        elif args:
            return f"Interactive prompt with {len(args)} argument(s) for system interaction."
        else:
            return "System prompt for user interaction and guidance."

    @staticmethod
    def add_metadata(prompts: List[Dict]) -> List[Dict]:
        """Add metadata to prompts."""
        enhanced = []

        for prompt in prompts:
            enhanced_prompt = prompt.copy()

            # Add argument count metadata
            enhanced_prompt['argument_count'] = len(prompt.get('arguments', []))

            # Add complexity indicator
            enhanced_prompt['complexity'] = PromptsEnhancer._get_complexity_level(prompt)

            # Add usage hint
            enhanced_prompt['usage_hint'] = PromptsEnhancer._get_usage_hint(prompt)

            enhanced.append(enhanced_prompt)

        return enhanced

    @staticmethod
    def _get_complexity_level(prompt: Dict) -> str:
        """Get complexity level of a prompt."""
        args = prompt.get('arguments', [])
        if len(args) == 0:
            return "Simple"
        elif len(args) <= 3:
            return "Moderate"
        else:
            return "Complex"

    @staticmethod
    def _get_usage_hint(prompt: Dict) -> str:
        """Get usage hint for a prompt."""
        name = prompt.get('name', '').lower()
        args = prompt.get('arguments', [])

        if 'memory' in name:
            return "Use for memory system guidance and tips"
        elif 'graph' in name:
            return "Use for graph operations and data queries"
        elif args:
            return f"Interactive prompt requiring {len(args)} input(s)"
        else:
            return "General purpose system prompt"
