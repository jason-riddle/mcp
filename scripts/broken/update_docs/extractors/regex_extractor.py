"""
Regex-based data extraction from Java source files.
"""

import re
from typing import Dict, List
from pathlib import Path

from .base import DataExtractor, ParameterInfo, ToolInfo, ResourceInfo, ExtractionError


class RegexDataExtractor(DataExtractor):
    """Extract MCP tools and resources using regex patterns."""

    def extract_tools(self) -> List[Dict]:
        """Extract tools from McpMemoryTools.java using regex parsing."""
        tools_file = self.config.get_tools_file_path()
        if not tools_file.exists():
            raise ExtractionError(f"Tools file not found: {tools_file}")

        content = tools_file.read_text()
        tools = []

        # Split by @Tool annotations to process each method independently
        tool_sections = re.split(r'(?=@Tool\()', content)

        for section in tool_sections:
            if not section.strip() or not section.startswith('@Tool('):
                continue

            try:
                tool_info = self._parse_tool_section(section)
                if tool_info:
                    tools.append(tool_info.to_dict(self.config))
            except Exception as e:
                # Log error but continue processing other tools
                print(f"Warning: Failed to parse tool section: {e}")
                continue

        return tools

    def extract_resources(self) -> List[Dict]:
        """Extract resources from McpMemoryResources.java using regex parsing."""
        resources_file = self.config.get_resources_file_path()
        if not resources_file.exists():
            raise ExtractionError(f"Resources file not found: {resources_file}")

        content = resources_file.read_text()
        resources = []

        # Find all @Resource annotations and their methods
        resource_pattern = r'@Resource\(uri\s*=\s*"([^"]+)"\)\s*TextResourceContents\s+(\w+)\s*\(\)'

        for match in re.finditer(resource_pattern, content):
            uri = match.group(1)
            method_name = match.group(2)

            # Extract javadoc description
            description = self._extract_javadoc_description(content, match.start())

            resource_info = ResourceInfo(uri, method_name, description)
            resources.append(resource_info.to_dict())

        return resources

    def extract_prompts(self) -> List[Dict]:
        """Extract prompt information from source files using regex patterns.

        Returns:
            List of dictionaries containing prompt information.
        """
        # For now, return empty list as prompts might not be implemented yet
        # This can be extended when @Prompt annotations are added to the Java code
        return []

    def _parse_tool_section(self, section: str) -> ToolInfo:
        """Parse a single tool section containing @Tool annotation and method."""
        # Extract tool annotation details
        tool_match = re.search(r'@Tool\(name\s*=\s*"([^"]+)",\s*description\s*=\s*"([^"]+)"\)', section)
        if not tool_match:
            raise ExtractionError("Could not find @Tool annotation")

        tool_name = tool_match.group(1)
        description = tool_match.group(2)

        # Find the method signature
        method_match = re.search(r'public\s+(\w+(?:<[^>]+>)?)\s+(\w+)\s*\(([^)]*)\)', section)
        if not method_match:
            raise ExtractionError("Could not find method signature")

        return_type = method_match.group(1)
        method_name = method_match.group(2)

        # Parse parameters from the entire method section
        parameters = self._parse_method_parameters(section)

        return ToolInfo(tool_name, description, method_name, return_type, parameters)

    def _parse_method_parameters(self, method_section: str) -> List[ParameterInfo]:
        """Parse parameters from a method section including annotations."""
        if not method_section.strip():
            return []

        parameters = []

        # Find @ToolArg annotations followed by parameter declarations
        toolarg_pattern = r'@ToolArg\(description\s*=\s*"([^"]+)"\)\s+final\s+([^)]+?)\s+(\w+)\s*(?=[,)])'

        for match in re.finditer(toolarg_pattern, method_section, re.MULTILINE | re.DOTALL):
            description = match.group(1)
            param_type = match.group(2).strip()
            param_name = match.group(3)

            # Clean up the type (remove newlines and extra spaces)
            param_type = ' '.join(param_type.split())

            # Simplify Java type to schema type
            simplified_type = self.config.simplify_java_type(param_type)

            parameters.append(ParameterInfo(param_name, simplified_type, description, True))

        return parameters

    def _extract_javadoc_description(self, content: str, position: int) -> str:
        """Extract javadoc comment before the given position."""
        before_content = content[:position]
        lines = before_content.split('\n')

        javadoc_lines = []
        found_javadoc = False

        for line in reversed(lines):
            line = line.strip()
            if line.endswith('*/'):
                found_javadoc = True
                if line != '*/':
                    javadoc_lines.append(line[:-2].strip())
                continue
            elif found_javadoc and line.startswith('*'):
                content_line = line[1:].strip()
                if content_line:
                    javadoc_lines.append(content_line)
            elif found_javadoc and line.startswith('/**'):
                if len(line) > 3:
                    javadoc_lines.append(line[3:].strip())
                break
            elif found_javadoc:
                break

        if not found_javadoc:
            return ""

        javadoc_lines.reverse()
        description_lines = []

        for line in javadoc_lines:
            if line.startswith('@param') or line.startswith('@return'):
                break
            if line:
                description_lines.append(line)

        return ' '.join(description_lines)


class RegexPatterns:
    """Common regex patterns for parsing Java source code."""

    # Tool annotation pattern
    TOOL_ANNOTATION = r'@Tool\(name\s*=\s*"([^"]+)",\s*description\s*=\s*"([^"]+)"\)'

    # Resource annotation pattern
    RESOURCE_ANNOTATION = r'@Resource\(uri\s*=\s*"([^"]+)"\)'

    # Method signature pattern
    METHOD_SIGNATURE = r'public\s+(\w+(?:<[^>]+>)?)\s+(\w+)\s*\(([^)]*)\)'

    # Parameter annotation pattern
    TOOL_ARG_ANNOTATION = r'@ToolArg\(description\s*=\s*"([^"]+)"\)\s+final\s+([^)]+?)\s+(\w+)\s*(?=[,)])'

    # Javadoc patterns
    JAVADOC_START = r'/\*\*'
    JAVADOC_END = r'\*/'
    JAVADOC_LINE = r'\s*\*\s*(.*)'

    @classmethod
    def find_tool_sections(cls, content: str) -> List[str]:
        """Split content into tool sections by @Tool annotations."""
        return re.split(r'(?=@Tool\()', content)

    @classmethod
    def extract_tool_annotation(cls, section: str) -> tuple:
        """Extract name and description from @Tool annotation."""
        match = re.search(cls.TOOL_ANNOTATION, section)
        if match:
            return match.group(1), match.group(2)
        return None, None

    @classmethod
    def extract_method_signature(cls, section: str) -> tuple:
        """Extract return type, method name, and parameters string from method signature."""
        match = re.search(cls.METHOD_SIGNATURE, section)
        if match:
            return match.group(1), match.group(2), match.group(3)
        return None, None, None

    @classmethod
    def find_tool_arg_annotations(cls, section: str) -> List[tuple]:
        """Find all @ToolArg annotations with their parameter information."""
        matches = re.finditer(cls.TOOL_ARG_ANNOTATION, section, re.MULTILINE | re.DOTALL)
        return [(m.group(1), m.group(2).strip(), m.group(3)) for m in matches]
