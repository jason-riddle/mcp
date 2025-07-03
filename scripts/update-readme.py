#!/usr/bin/env python3
"""
Copyright (c) 2025 Jason Riddle.

Licensed under the MIT License.
"""

import re
import sys
from pathlib import Path
from typing import Dict, List


class JavaDocParser:
    """Parser for extracting MCP tools and resources from Java source files."""

    def __init__(self, project_root: Path):
        self.project_root = project_root
        self.src_dir = project_root / "src" / "main" / "java" / "com" / "jasonriddle" / "mcp"

    def parse_tools(self) -> List[Dict]:
        """Parse MCP tools from McpMemoryTools.java"""
        tools_file = self.src_dir / "McpMemoryTools.java"
        if not tools_file.exists():
            return []

        content = tools_file.read_text()
        tools = []

        # Find all @Tool annotations and their methods
        tool_pattern = r'@Tool\(name\s*=\s*"([^"]+)",\s*description\s*=\s*"([^"]+)"\)\s*public\s+(\w+)\s+(\w+)\s*\(([^)]*)\)'

        for match in re.finditer(tool_pattern, content, re.MULTILINE | re.DOTALL):
            tool_name = match.group(1)
            description = match.group(2)
            return_type = match.group(3)
            method_name = match.group(4)
            params_str = match.group(5)

            # Parse parameters
            parameters = self._parse_parameters(params_str, content, method_name)

            # Extract javadoc
            javadoc = self._extract_javadoc(content, match.start())

            tools.append({
                'name': tool_name,
                'title': self._format_title(tool_name),
                'description': description,
                'method_name': method_name,
                'return_type': return_type,
                'parameters': parameters,
                'javadoc': javadoc,
                'read_only': return_type != 'String' and 'delete' not in tool_name.lower()
            })

        return tools

    def parse_resources(self) -> List[Dict]:
        """Parse MCP resources from McpMemoryResources.java"""
        resources_file = self.src_dir / "McpMemoryResources.java"
        if not resources_file.exists():
            return []

        content = resources_file.read_text()
        resources = []

        # Find all @Resource annotations and their methods
        resource_pattern = r'@Resource\(uri\s*=\s*"([^"]+)"\)\s*TextResourceContents\s+(\w+)\s*\(\)'

        for match in re.finditer(resource_pattern, content):
            uri = match.group(1)
            method_name = match.group(2)

            # Extract javadoc
            javadoc = self._extract_javadoc(content, match.start())

            resources.append({
                'uri': uri,
                'method_name': method_name,
                'title': self._format_resource_title(uri),
                'description': javadoc.get('description', ''),
                'javadoc': javadoc
            })

        return resources

    def _parse_parameters(self, params_str: str, content: str, method_name: str) -> List[Dict]:
        """Parse method parameters from parameter string"""
        if not params_str.strip():
            return []

        parameters = []

        # Look for @ToolArg annotations in the method
        method_start = content.find(f'public String {method_name}(')
        if method_start == -1:
            method_start = content.find(f'public MemoryGraph {method_name}(')

        if method_start != -1:
            method_end = content.find(')', method_start)
            method_section = content[method_start:method_end + 100]

            # Find @ToolArg annotations
            toolarg_pattern = r'@ToolArg\(description\s*=\s*"([^"]+)"\)\s*final\s+(\w+(?:<[^>]+>)?)\s+(\w+)'

            for match in re.finditer(toolarg_pattern, method_section):
                description = match.group(1)
                param_type = match.group(2)
                param_name = match.group(3)

                parameters.append({
                    'name': param_name,
                    'type': self._simplify_type(param_type),
                    'description': description,
                    'required': True  # All parameters in this codebase appear to be required
                })

        return parameters

    def _extract_javadoc(self, content: str, position: int) -> Dict:
        """Extract javadoc comment before the given position"""
        # Look backwards for the javadoc comment
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
            return {}

        javadoc_lines.reverse()

        # Parse javadoc content
        description_lines = []
        params = {}
        return_desc = ""

        current_section = "description"

        for line in javadoc_lines:
            if line.startswith('@param'):
                current_section = "param"
                param_match = re.match(r'@param\s+(\w+)\s+(.+)', line)
                if param_match:
                    params[param_match.group(1)] = param_match.group(2)
            elif line.startswith('@return'):
                current_section = "return"
                return_desc = line[7:].strip()
            elif current_section == "description" and line:
                description_lines.append(line)

        return {
            'description': ' '.join(description_lines),
            'params': params,
            'return': return_desc
        }

    def _format_title(self, tool_name: str) -> str:
        """Format tool name into a readable title"""
        parts = tool_name.replace('memory.', '').split('_')
        return ' '.join(word.capitalize() for word in parts)

    def _format_resource_title(self, uri: str) -> str:
        """Format resource URI into a readable title"""
        resource_name = uri.replace('memory://', '')
        return f"Memory {resource_name.capitalize()}"

    def _simplify_type(self, java_type: str) -> str:
        """Simplify Java type to more readable format"""
        type_mapping = {
            'String': 'string',
            'List<String>': 'array',
            'List<Map<String, Object>>': 'array',
            'List<Map<String, String>>': 'array',
            'Map<String, Object>': 'object',
            'Map<String, String>': 'object',
            'boolean': 'boolean',
            'int': 'number',
            'long': 'number'
        }
        return type_mapping.get(java_type, java_type.lower())


class ReadmeUpdater:
    """Updates README.md with generated tool and resource documentation."""

    def __init__(self, project_root: Path):
        self.project_root = project_root
        self.readme_path = project_root / "README.md"
        self.parser = JavaDocParser(project_root)

    def update_readme(self):
        """Update README.md with tool and resource documentation"""
        print("Parsing MCP tools and resources from Java source files...")

        tools = self.parser.parse_tools()
        resources = self.parser.parse_resources()

        print(f"Found {len(tools)} tools and {len(resources)} resources")

        if not self.readme_path.exists():
            print(f"ERROR: README.md not found at {self.readme_path}")
            return False

        content = self.readme_path.read_text()

        # Update tools section
        content = self._update_tools_section(content, tools)

        # Update resources section
        content = self._update_resources_section(content, resources)

        # Write updated content
        self.readme_path.write_text(content)
        print("README.md updated successfully")
        return True

    def _update_tools_section(self, content: str, tools: List[Dict]) -> str:
        """Update the tools section in README"""
        tools_content = self._generate_tools_content(tools)
        return self._update_section(content, "tools", tools_content)

    def _update_resources_section(self, content: str, resources: List[Dict]) -> str:
        """Update the resources section in README"""
        resources_content = self._generate_resources_content(resources)
        return self._update_section(content, "resources", resources_content)

    def _update_section(self, content: str, section_type: str, new_content: str) -> str:
        """Update a section between markers in the content"""
        import re

        # Look for flexible start marker pattern
        start_pattern = f"<!--- {section_type.capitalize()} generated by .* -->"
        end_marker = f"<!--- End of {section_type} generated section -->"

        # Find the start marker using regex
        start_match = re.search(start_pattern, content)
        if not start_match:
            print(f"WARNING: Start marker pattern '{start_pattern}' not found in README")
            return content

        start_idx = start_match.end()
        end_idx = content.find(end_marker)

        if end_idx == -1:
            print(f"WARNING: End marker '{end_marker}' not found in README")
            return content

        # Generate new start marker
        new_start_marker = f"<!--- {section_type.capitalize()} generated by update-readme.py -->"

        return (
            content[:start_match.start()] +
            new_start_marker + "\n\n" + new_content + "\n\n" +
            content[end_idx:]
        )

    def _generate_tools_content(self, tools: List[Dict]) -> str:
        """Generate formatted content for tools section"""
        if not tools:
            return "No tools found."

        lines = []

        # Group tools by category
        categories = {
            'Entity Management': [t for t in tools if any(x in t['name'] for x in ['create_entities', 'delete_entities'])],
            'Relationship Management': [t for t in tools if any(x in t['name'] for x in ['create_relations', 'delete_relations'])],
            'Observation Management': [t for t in tools if any(x in t['name'] for x in ['add_observations', 'delete_observations'])],
            'Graph Operations': [t for t in tools if any(x in t['name'] for x in ['read_graph', 'search_nodes', 'open_nodes'])]
        }

        for category, category_tools in categories.items():
            if not category_tools:
                continue

            lines.append(f"<details>")
            lines.append(f"<summary><b>{category}</b></summary>")
            lines.append("")

            for tool in category_tools:
                lines.extend(self._format_tool(tool))

            lines.append("</details>")
            lines.append("")

        return '\n'.join(lines)

    def _generate_resources_content(self, resources: List[Dict]) -> str:
        """Generate formatted content for resources section"""
        if not resources:
            return "No resources found."

        lines = ["<details>", "<summary><b>Memory Resources</b></summary>", ""]

        for resource in resources:
            lines.extend(self._format_resource(resource))

        lines.extend(["</details>", ""])

        return '\n'.join(lines)

    def _format_tool(self, tool: Dict) -> List[str]:
        """Format a single tool for documentation"""
        lines = [
            f"<!-- NOTE: This has been generated via update-readme.py -->",
            "",
            f"- **{tool['name']}**",
            f"  - Title: {tool['title']}",
            f"  - Description: {tool['description']}"
        ]

        if tool['parameters']:
            lines.append("  - Parameters:")
            for param in tool['parameters']:
                required_text = "" if param['required'] else ", optional"
                lines.append(f"    - `{param['name']}` ({param['type']}{required_text}): {param['description']}")
        else:
            lines.append("  - Parameters: None")

        lines.append(f"  - Read-only: **{tool['read_only']}**")
        lines.append("")

        return lines

    def _format_resource(self, resource: Dict) -> List[str]:
        """Format a single resource for documentation"""
        return [
            f"<!-- NOTE: This has been generated via update-readme.py -->",
            "",
            f"- **{resource['uri']}**",
            f"  - Title: {resource['title']}",
            f"  - Description: {resource['description']}",
            ""
        ]


def main():
    """Main entry point"""
    import argparse
    import json

    parser = argparse.ArgumentParser(description='Update README with MCP tools and resources documentation')
    parser.add_argument('--json', action='store_true', help='Output JSON instead of updating README')
    args = parser.parse_args()

    script_dir = Path(__file__).parent
    project_root = script_dir.parent

    updater = ReadmeUpdater(project_root)

    try:
        if args.json:
            # Output JSON data
            tools = updater.parser.parse_tools()
            resources = updater.parser.parse_resources()

            output = {
                'tools': tools,
                'resources': resources
            }

            print(json.dumps(output, indent=2))
            sys.exit(0)
        else:
            # Update README
            success = updater.update_readme()
            sys.exit(0 if success else 1)
    except Exception as e:
        print(f"ERROR: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
