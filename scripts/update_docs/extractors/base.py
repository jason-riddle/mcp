"""
Base classes and interfaces for data extraction.
"""

from abc import ABC, abstractmethod
from typing import Dict, List
from pathlib import Path

from ..config import DocumentationConfig


class DataExtractor(ABC):
    """Abstract base class for extracting documentation data from source files."""

    def __init__(self, config: DocumentationConfig):
        """Initialize extractor with configuration."""
        self.config = config

    @abstractmethod
    def extract_tools(self) -> List[Dict]:
        """Extract tool information from source files.

        Returns:
            List of dictionaries containing tool information with keys:
            - name: Tool name (e.g., "memory.create_entities")
            - title: Human-readable title
            - description: Tool description
            - method_name: Java method name
            - return_type: Java return type
            - parameters: List of parameter dictionaries
            - read_only: Boolean indicating if tool is read-only
        """
        pass

    @abstractmethod
    def extract_resources(self) -> List[Dict]:
        """Extract resource information from source files.

        Returns:
            List of dictionaries containing resource information with keys:
            - uri: Resource URI (e.g., "memory://graph")
            - title: Human-readable title
            - description: Resource description
            - method_name: Java method name
        """
        pass

    @abstractmethod
    def extract_prompts(self) -> List[Dict]:
        """Extract prompt information from source files.

        Returns:
            List of dictionaries containing prompt information with keys:
            - name: Prompt name
            - title: Prompt title (optional)
            - description: Prompt description
            - arguments: List of argument dictionaries (optional)
        """
        pass

    def extract_all(self) -> Dict:
        """Extract tools, resources, and prompts data.

        Returns:
            Dictionary with 'tools', 'resources', and 'prompts' keys containing
            the respective extracted data.
        """
        return {
            'tools': self.extract_tools(),
            'resources': self.extract_resources(),
            'prompts': self.extract_prompts()
        }

    def format_tool_title(self, tool_name: str) -> str:
        """Format tool name into a readable title."""
        parts = tool_name.replace('memory.', '').split('_')
        return ' '.join(word.capitalize() for word in parts)

    def format_resource_title(self, uri: str) -> str:
        """Format resource URI into a readable title."""
        resource_name = uri.replace('memory://', '')
        return f"Memory {resource_name.capitalize()}"

    def is_read_only_tool(self, tool_name: str, return_type: str) -> bool:
        """Determine if a tool is read-only based on name and return type."""
        return (return_type != 'String' or
                'read' in tool_name.lower() or
                'search' in tool_name.lower() or
                'open' in tool_name.lower())


class ExtractionError(Exception):
    """Exception raised when data extraction fails."""
    pass


class ParameterInfo:
    """Container for parameter information."""

    def __init__(self, name: str, param_type: str, description: str, required: bool = True):
        self.name = name
        self.param_type = param_type
        self.description = description
        self.required = required

    def to_dict(self) -> Dict:
        """Convert to dictionary representation."""
        return {
            'name': self.name,
            'type': self.param_type,
            'description': self.description,
            'required': self.required
        }


class ToolInfo:
    """Container for tool information."""

    def __init__(self, name: str, description: str, method_name: str,
                 return_type: str, parameters: List[ParameterInfo]):
        self.name = name
        self.description = description
        self.method_name = method_name
        self.return_type = return_type
        self.parameters = parameters

    def to_dict(self, config: DocumentationConfig) -> Dict:
        """Convert to dictionary representation."""
        return {
            'name': self.name,
            'title': self._format_title(),
            'description': self.description,
            'method_name': self.method_name,
            'return_type': self.return_type,
            'parameters': [param.to_dict() for param in self.parameters],
            'read_only': self._is_read_only()
        }

    def _format_title(self) -> str:
        """Format tool name into a readable title."""
        parts = self.name.replace('memory.', '').split('_')
        return ' '.join(word.capitalize() for word in parts)

    def _is_read_only(self) -> bool:
        """Determine if tool is read-only."""
        return (self.return_type != 'String' or
                'read' in self.name.lower() or
                'search' in self.name.lower() or
                'open' in self.name.lower())


class ResourceInfo:
    """Container for resource information."""

    def __init__(self, uri: str, method_name: str, description: str = ""):
        self.uri = uri
        self.method_name = method_name
        self.description = description

    def to_dict(self) -> Dict:
        """Convert to dictionary representation."""
        return {
            'uri': self.uri,
            'title': self._format_title(),
            'description': self.description or self._generate_description(),
            'method_name': self.method_name
        }

    def _format_title(self) -> str:
        """Format resource URI into a readable title."""
        resource_name = self.uri.replace('memory://', '')
        return f"Memory {resource_name.capitalize()}"

    def _generate_description(self) -> str:
        """Generate a default description if none provided."""
        return f"Resource for {self._format_title().lower()}"


class PromptInfo:
    """Container for prompt information."""

    def __init__(self, name: str, description: str = "", title: str = "", arguments: List[Dict] = None):
        self.name = name
        self.description = description
        self.title = title
        self.arguments = arguments or []

    def to_dict(self) -> Dict:
        """Convert to dictionary representation."""
        return {
            'name': self.name,
            'description': self.description,
            'title': self.title,
            'arguments': self.arguments
        }
