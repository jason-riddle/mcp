"""
Resources section markdown generator.
"""

from typing import Dict, List
from ..config import DocumentationConfig


class ResourcesGenerator:
    """Generate formatted markdown content for resources section."""

    def __init__(self, config: DocumentationConfig):
        """Initialize resources generator with configuration."""
        self.config = config

    def generate_section(self, resources: List[Dict]) -> str:
        """Generate complete resources section markdown."""
        if not resources:
            return "No resources found."

        lines = ["<details>", "<summary><b>Memory Resources</b></summary>", ""]

        # Sort resources by URI for consistent output
        sorted_resources = sorted(resources, key=lambda r: r.get('uri', ''))

        for resource in sorted_resources:
            lines.extend(self._format_resource(resource))

        lines.extend(["</details>", ""])

        return '\n'.join(lines)

    def _format_resource(self, resource: Dict) -> List[str]:
        """Format a single resource for documentation."""
        return [
            "<!-- NOTE: This has been generated via update-docs.py --->",
            "",
            f"- **{resource['uri']}**",
            f"  - Title: {resource['title']}",
            f"  - Description: {resource['description']}",
            ""
        ]


class ResourcesFormatter:
    """Helper class for formatting resource information."""

    @staticmethod
    def format_resource_header(resource: Dict) -> str:
        """Format resource header with URI and title."""
        return f"**{resource['uri']}** - {resource['title']}"

    @staticmethod
    def format_uri(uri: str) -> str:
        """Format resource URI with proper escaping."""
        # Ensure URI starts with memory://
        if not uri.startswith('memory://'):
            return f"memory://{uri}"
        return uri

    @staticmethod
    def clean_description(description: str) -> str:
        """Clean and format resource description."""
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


class ResourcesSorter:
    """Helper class for sorting resources in different ways."""

    @staticmethod
    def sort_by_uri(resources: List[Dict]) -> List[Dict]:
        """Sort resources alphabetically by URI."""
        return sorted(resources, key=lambda r: r.get('uri', ''))

    @staticmethod
    def sort_by_title(resources: List[Dict]) -> List[Dict]:
        """Sort resources alphabetically by title."""
        return sorted(resources, key=lambda r: r.get('title', ''))

    @staticmethod
    def sort_by_resource_type(resources: List[Dict]) -> List[Dict]:
        """Sort resources by type (extracted from URI)."""
        def get_resource_type(resource):
            uri = resource.get('uri', '')
            if uri.startswith('memory://'):
                return uri[9:]  # Remove 'memory://' prefix
            return uri

        return sorted(resources, key=get_resource_type)


class ResourcesValidator:
    """Helper class for validating resource data before generation."""

    @staticmethod
    def validate_resource(resource: Dict) -> List[str]:
        """Validate a single resource and return list of validation errors."""
        errors = []

        # Required fields
        required_fields = ['uri', 'title']
        for field in required_fields:
            if field not in resource or not resource[field]:
                errors.append(f"Missing or empty required field: {field}")

        # Validate URI format
        if 'uri' in resource:
            uri = resource['uri']
            if not uri.startswith('memory://'):
                errors.append(f"Resource URI should start with 'memory://': {uri}")

            # Check for valid characters in URI
            if not all(c.isalnum() or c in '://_-' for c in uri):
                errors.append(f"Resource URI contains invalid characters: {uri}")

        # Validate title format
        if 'title' in resource:
            title = resource['title']
            if not title.strip():
                errors.append("Resource title cannot be empty or whitespace only")

        return errors

    @staticmethod
    def validate_resources_list(resources: List[Dict]) -> List[str]:
        """Validate entire resources list."""
        errors = []

        if not resources:
            errors.append("Resources list is empty")
            return errors

        # Check for duplicate resource URIs
        uris = [resource.get('uri', '') for resource in resources]
        duplicates = set([uri for uri in uris if uris.count(uri) > 1])
        if duplicates:
            errors.append(f"Duplicate resource URIs found: {', '.join(duplicates)}")

        # Validate each resource
        for i, resource in enumerate(resources):
            resource_errors = ResourcesValidator.validate_resource(resource)
            errors.extend([f"Resource {i}: {error}" for error in resource_errors])

        return errors

    @staticmethod
    def get_common_resource_uris() -> List[str]:
        """Get list of commonly expected resource URIs."""
        return [
            'memory://graph',
            'memory://status',
            'memory://types'
        ]

    @staticmethod
    def check_expected_resources(resources: List[Dict]) -> List[str]:
        """Check if expected resources are present."""
        warnings = []

        expected_uris = ResourcesValidator.get_common_resource_uris()
        found_uris = set(resource.get('uri', '') for resource in resources)

        missing_uris = set(expected_uris) - found_uris
        if missing_uris:
            warnings.append(f"Missing expected resources: {', '.join(missing_uris)}")

        return warnings


class ResourcesEnhancer:
    """Helper class for enhancing resource data with additional information."""

    @staticmethod
    def enhance_descriptions(resources: List[Dict]) -> List[Dict]:
        """Enhance resource descriptions with better defaults."""
        enhanced = []

        for resource in resources:
            enhanced_resource = resource.copy()

            # Enhance description if empty or generic
            uri = resource.get('uri', '')
            description = resource.get('description', '')

            if not description or description in ['', 'No description available.']:
                enhanced_resource['description'] = ResourcesEnhancer._generate_description(uri)
            else:
                enhanced_resource['description'] = ResourcesFormatter.clean_description(description)

            enhanced.append(enhanced_resource)

        return enhanced

    @staticmethod
    def _generate_description(uri: str) -> str:
        """Generate a description based on URI."""
        if uri == 'memory://graph':
            return "Returns the complete knowledge graph in formatted, human-readable way."
        elif uri == 'memory://status':
            return "Returns memory graph status and health information."
        elif uri == 'memory://types':
            return "Returns types and patterns available in the memory graph."
        elif uri.startswith('memory://'):
            resource_type = uri[9:]  # Remove 'memory://' prefix
            return f"Returns {resource_type} information from the memory system."
        else:
            return "Memory resource providing system information."

    @staticmethod
    def add_metadata(resources: List[Dict]) -> List[Dict]:
        """Add metadata to resources."""
        enhanced = []

        for resource in resources:
            enhanced_resource = resource.copy()

            # Add resource type metadata
            uri = resource.get('uri', '')
            if uri.startswith('memory://'):
                enhanced_resource['resource_type'] = uri[9:]
            else:
                enhanced_resource['resource_type'] = 'unknown'

            # Add estimated content size hint
            enhanced_resource['content_hint'] = ResourcesEnhancer._get_content_hint(uri)

            enhanced.append(enhanced_resource)

        return enhanced

    @staticmethod
    def _get_content_hint(uri: str) -> str:
        """Get hint about expected content size/type."""
        if uri == 'memory://graph':
            return "Large JSON object with complete graph data"
        elif uri == 'memory://status':
            return "Small JSON object with status information"
        elif uri == 'memory://types':
            return "Array of available entity and relation types"
        else:
            return "Varies based on memory content"
