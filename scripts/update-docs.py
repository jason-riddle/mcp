#!/usr/bin/env python3
"""
Staged documentation generator for MCP tools and resources.
Single script that combines all approaches with validation and rollback.
"""

import json
import os
import shutil
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional


class StagedDocumentationGenerator:
    """Main class for staged documentation generation."""

    def __init__(self, project_root: Path):
        self.project_root = project_root
        self.staging_dir = project_root / "staging"
        self.readme_path = project_root / "README.md"
        self.src_dir = project_root / "src" / "main" / "java" / "com" / "jasonriddle" / "mcp"

        # Ensure staging directory exists
        self.staging_dir.mkdir(exist_ok=True)

    def run(self) -> bool:
        """Run the complete staged documentation generation process."""
        print("Starting staged documentation generation...")

        try:
            # Stage 1: Data Extraction
            print("\n=== Stage 1: Data Extraction ===")
            data = self.extract_all_data()
            if not data:
                print("✗ Stage 1 failed: No data extracted")
                return False
            self.write_json("extracted-data.json", data)
            print(f"✓ Stage 1 complete: {len(data.get('tools', []))} tools, {len(data.get('resources', []))} resources")

            # Stage 2: Content Generation
            print("\n=== Stage 2: Content Generation ===")
            content = self.generate_all_content(data)
            if not content:
                print("✗ Stage 2 failed: No content generated")
                return False
            self.write_json("generated-content.json", content)
            print("✓ Stage 2 complete: Generated tools and resources sections")

            # Stage 3: Staging File Creation
            print("\n=== Stage 3: Staging File Creation ===")
            staging_readme = self.create_staging_readme(content)
            if not staging_readme:
                print("✗ Stage 3 failed: Could not create staging README")
                return False

            staging_path = self.staging_dir / "README-staging.md"
            with open(staging_path, 'w') as f:
                f.write(staging_readme)
            print(f"✓ Stage 3 complete: Created {staging_path}")

            # Stage 4: Validation
            print("\n=== Stage 4: Validation ===")
            validation_result = self.validate_staging_readme(staging_path)
            self.write_validation_report(validation_result)
            if not validation_result['valid']:
                print("✗ Stage 4 failed: Validation errors found")
                return False
            print("✓ Stage 4 complete: Validation passed")

            # Stage 5: Apply Changes
            print("\n=== Stage 5: Apply Changes ===")
            backup_path = self.backup_readme()
            if not backup_path:
                print("✗ Stage 5 failed: Could not create backup")
                return False

            if not self.apply_staging_changes(staging_path):
                print("✗ Stage 5 failed: Could not apply changes")
                print(f"Original README backup available at: {backup_path}")
                return False

            print("✓ Stage 5 complete: Changes applied successfully")
            print(f"✓ Backup created at: {backup_path}")

            return True

        except Exception as e:
            print(f"✗ Generation failed: {e}")
            return False

    def extract_all_data(self) -> Dict:
        """Stage 1: Extract all data using Java reflection with regex fallback."""
        print("Attempting Java reflection extraction...")

        # Method 1: Try Java reflection first (most accurate)
        try:
            result = subprocess.run([
                "java", "-cp", "scripts:target/classes:target/quarkus-app/lib/main/*",
                "UpdateReadme", "--json"
            ], capture_output=True, text=True, cwd=self.project_root)

            if result.returncode == 0:
                data = json.loads(result.stdout)
                print("✓ Java reflection extraction successful")
                return data
            else:
                print(f"Java reflection failed (exit code {result.returncode}): {result.stderr}")
        except Exception as e:
            print(f"Java reflection failed: {e}")

        # Method 2: Fallback to regex parsing
        print("Falling back to regex parsing...")
        return self.extract_with_regex()

    def extract_with_regex(self) -> Dict:
        """Fallback data extraction using regex parsing."""
        tools = self.parse_tools_with_regex()
        resources = self.parse_resources_with_regex()

        return {
            "tools": tools,
            "resources": resources
        }

    def parse_tools_with_regex(self) -> List[Dict]:
        """Parse tools from McpMemoryTools.java using regex."""
        tools_file = self.src_dir / "McpMemoryTools.java"
        if not tools_file.exists():
            print(f"Warning: {tools_file} not found")
            return []

        content = tools_file.read_text()
        tools = []

        # Find all @Tool annotations and their methods with a more robust pattern
        import re

        # Split by @Tool annotations to process each method independently
        tool_sections = re.split(r'(?=@Tool\()', content)

        for section in tool_sections:
            if not section.strip() or not section.startswith('@Tool('):
                continue

            # Extract tool annotation details
            tool_match = re.search(r'@Tool\(name\s*=\s*"([^"]+)",\s*description\s*=\s*"([^"]+)"\)', section)
            if not tool_match:
                continue

            tool_name = tool_match.group(1)
            description = tool_match.group(2)

            # Find the method signature - handle both empty and non-empty parameter lists
            method_match = re.search(r'public\s+(\w+(?:<[^>]+>)?)\s+(\w+)\s*\(([^)]*)\)', section)
            if not method_match:
                continue

            return_type = method_match.group(1)
            method_name = method_match.group(2)
            params_section = method_match.group(3)

            # Parse parameters from the entire method section
            parameters = self.parse_method_parameters(section)

            tools.append({
                'name': tool_name,
                'title': self.format_title(tool_name),
                'description': description,
                'method_name': method_name,
                'return_type': return_type,
                'parameters': parameters,
                'read_only': self.is_read_only_tool(tool_name, return_type)
            })

        return tools

    def parse_resources_with_regex(self) -> List[Dict]:
        """Parse resources from McpMemoryResources.java using regex."""
        resources_file = self.src_dir / "McpMemoryResources.java"
        if not resources_file.exists():
            print(f"Warning: {resources_file} not found")
            return []

        content = resources_file.read_text()
        resources = []

        # Find all @Resource annotations and their methods
        import re
        resource_pattern = r'@Resource\(uri\s*=\s*"([^"]+)"\)\s*TextResourceContents\s+(\w+)\s*\(\)'

        for match in re.finditer(resource_pattern, content):
            uri = match.group(1)
            method_name = match.group(2)

            resources.append({
                'uri': uri,
                'method_name': method_name,
                'title': self.format_resource_title(uri),
                'description': self.extract_javadoc_description(content, match.start())
            })

        return resources

    def parse_method_parameters(self, method_section: str) -> List[Dict]:
        """Parse parameters from a method section (includes annotations and method body)."""
        if not method_section.strip():
            return []

        parameters = []
        import re

        # Find @ToolArg annotations followed by parameter declarations
        # This pattern handles multi-line parameter declarations and whitespace
        toolarg_pattern = r'@ToolArg\(description\s*=\s*"([^"]+)"\)\s+final\s+([^)]+?)\s+(\w+)\s*(?=[,)])'

        for match in re.finditer(toolarg_pattern, method_section, re.MULTILINE | re.DOTALL):
            description = match.group(1)
            param_type = match.group(2).strip()
            param_name = match.group(3)

            # Clean up the type (remove newlines and extra spaces)
            param_type = ' '.join(param_type.split())

            parameters.append({
                'name': param_name,
                'type': self.simplify_type(param_type),
                'description': description,
                'required': True
            })

        return parameters

    def extract_javadoc_description(self, content: str, position: int) -> str:
        """Extract javadoc comment before the given position."""
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
            return ""

        javadoc_lines.reverse()
        description_lines = []

        for line in javadoc_lines:
            if line.startswith('@param') or line.startswith('@return'):
                break
            if line:
                description_lines.append(line)

        return ' '.join(description_lines)

    def format_title(self, tool_name: str) -> str:
        """Format tool name into a readable title."""
        parts = tool_name.replace('memory.', '').split('_')
        return ' '.join(word.capitalize() for word in parts)

    def format_resource_title(self, uri: str) -> str:
        """Format resource URI into a readable title."""
        resource_name = uri.replace('memory://', '')
        return f"Memory {resource_name.capitalize()}"

    def simplify_type(self, java_type: str) -> str:
        """Simplify Java type to more readable format."""
        # Remove extra spaces
        java_type = java_type.strip()

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

        # Also handle types without spaces after commas
        normalized_type = java_type.replace('Map<String,Object>', 'Map<String, Object>')
        normalized_type = normalized_type.replace('Map<String,String>', 'Map<String, String>')
        normalized_type = normalized_type.replace('List<Map<String,Object>>', 'List<Map<String, Object>>')
        normalized_type = normalized_type.replace('List<Map<String,String>>', 'List<Map<String, String>>')

        return type_mapping.get(normalized_type, normalized_type.lower())

    def is_read_only_tool(self, tool_name: str, return_type: str) -> bool:
        """Determine if a tool is read-only."""
        return (return_type != 'String' or
                'read' in tool_name.lower() or
                'search' in tool_name.lower() or
                'open' in tool_name.lower())

    def write_json(self, filename: str, data: Dict) -> None:
        """Write data to a JSON file in the staging directory."""
        filepath = self.staging_dir / filename
        with open(filepath, 'w') as f:
            json.dump(data, f, indent=2)
        print(f"✓ Wrote {filepath}")

    def generate_all_content(self, data: Dict) -> Dict:
        """Stage 2: Generate formatted content for tools and resources."""
        tools = data.get('tools', [])
        resources = data.get('resources', [])

        # Clean up resource descriptions (remove verbose Java source code)
        cleaned_resources = []
        for resource in resources:
            cleaned_resource = resource.copy()
            # Use the title to generate a clean description
            if resource['uri'] == 'memory://graph':
                cleaned_resource['description'] = "Returns the complete knowledge graph in formatted, human-readable way."
            elif resource['uri'] == 'memory://types':
                cleaned_resource['description'] = "Returns types and patterns available in the memory graph."
            elif resource['uri'] == 'memory://status':
                cleaned_resource['description'] = "Returns memory graph status and health information."
            else:
                # Fallback: try to extract first sentence from description
                desc = resource.get('description', '')
                if desc:
                    first_sentence = desc.split('.')[0] + '.'
                    if len(first_sentence) < 200:  # Only use if reasonably short
                        cleaned_resource['description'] = first_sentence
                    else:
                        cleaned_resource['description'] = f"Resource for {resource['title'].lower()}"
                else:
                    cleaned_resource['description'] = f"Resource for {resource['title'].lower()}"
            cleaned_resources.append(cleaned_resource)

        # Generate content sections
        tools_content = self.generate_tools_section(tools)
        resources_content = self.generate_resources_section(cleaned_resources)

        return {
            'tools_section': tools_content,
            'resources_section': resources_content,
            'metadata': {
                'generated_at': datetime.now().isoformat(),
                'tools_count': len(tools),
                'resources_count': len(cleaned_resources)
            }
        }

    def generate_tools_section(self, tools: List[Dict]) -> str:
        """Generate formatted content for the tools section."""
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

        first_category = True
        for category, category_tools in categories.items():
            if not category_tools:
                continue

            # Add spacing before category headers (except the first one)
            if not first_category:
                lines.append("")

            lines.append(f"#### {category}")
            lines.append("")
            first_category = False

            for i, tool in enumerate(category_tools):
                lines.extend(self.format_tool(tool))
                # Add spacing between tools within the same category, but not after the last tool
                if i < len(category_tools) - 1:
                    lines.append("")

        return '\n'.join(lines)

    def generate_resources_section(self, resources: List[Dict]) -> str:
        """Generate formatted content for the resources section."""
        if not resources:
            return "No resources found."

        lines = []

        for i, resource in enumerate(resources):
            lines.extend(self.format_resource(resource))
            # Add spacing between resources, but not after the last resource
            if i < len(resources) - 1:
                lines.append("")

        return '\n'.join(lines)

    def format_tool(self, tool: Dict) -> List[str]:
        """Format a single tool for documentation."""
        lines = [
            "<!-- NOTE: This has been generated via update-docs.py --->",
            "",
            f"- **{tool['name']}**",
            f"  - Title: {tool['title']}",
            f"  - Description: {tool['description']}"
        ]

        if tool.get('parameters'):
            lines.append("  - Parameters:")
            for param in tool['parameters']:
                required_text = "" if param.get('required', True) else ", optional"
                lines.append(f"    - `{param['name']}` ({param['type']}{required_text}): {param['description']}")
        else:
            lines.append("  - Parameters: None")

        lines.append(f"  - Read-only: **{str(tool.get('read_only', False)).lower()}**")

        return lines

    def format_resource(self, resource: Dict) -> List[str]:
        """Format a single resource for documentation."""
        return [
            "<!-- NOTE: This has been generated via update-docs.py --->",
            "",
            f"- **{resource['uri']}**",
            f"  - Title: {resource['title']}",
            f"  - Description: {resource['description']}"
        ]

    def create_staging_readme(self, content: Dict) -> str:
        """Stage 3: Create complete staging README with updated content."""
        if not self.readme_path.exists():
            print(f"ERROR: Original README.md not found at {self.readme_path}")
            return ""

        # Read the current README
        original_content = self.readme_path.read_text()

        # Update tools section
        updated_content = self.update_section_in_content(
            original_content,
            "tools",
            content['tools_section']
        )

        # Update resources section
        updated_content = self.update_section_in_content(
            updated_content,
            "resources",
            content['resources_section']
        )

        return updated_content

    def update_section_in_content(self, content: str, section_type: str, new_content: str) -> str:
        """Update a section between markers in the content."""
        import re

        # Look for flexible start marker pattern
        start_pattern = f"<!--- {section_type.capitalize()} generated by .* -->"
        end_marker = f"<!--- End of {section_type} generated section -->"

        # Find the start marker using regex
        start_match = re.search(start_pattern, content)
        if not start_match:
            print(f"[WARNING] Start marker pattern '{start_pattern}' not found - section not updated")
            return content

        start_idx = start_match.end()
        end_idx = content.find(end_marker)

        if end_idx == -1:
            print(f"[WARNING] End marker '{end_marker}' not found - section not updated")
            return content

        # Generate new start marker
        new_start_marker = f"<!--- {section_type.capitalize()} generated by update-docs.py -->"

        return (
            content[:start_match.start()] +
            new_start_marker + "\n\n" + new_content + "\n\n" +
            content[end_idx:]
        )

    def validate_staging_readme(self, staging_path: Path) -> Dict:
        """Stage 4: Validate the staging README file."""
        validation_result = {
            'valid': True,
            'errors': [],
            'warnings': [],
            'info': []
        }

        if not staging_path.exists():
            validation_result['valid'] = False
            validation_result['errors'].append(f"Staging file not found: {staging_path}")
            return validation_result

        try:
            content = staging_path.read_text()

            # Basic structure validation
            self.validate_markdown_structure(content, validation_result)

            # Content validation
            self.validate_content_integrity(content, validation_result)

            # Section marker validation
            self.validate_section_markers(content, validation_result)

            # File size validation
            self.validate_file_size(content, validation_result)

        except Exception as e:
            validation_result['valid'] = False
            validation_result['errors'].append(f"Failed to read staging file: {e}")

        return validation_result

    def validate_markdown_structure(self, content: str, result: Dict) -> None:
        """Validate basic markdown structure."""
        lines = content.split('\n')

        # Check for title
        if not lines[0].startswith('# '):
            result['errors'].append("Missing main title (should start with '# ')")
            result['valid'] = False

        # Check for balanced code blocks
        code_block_count = content.count('```')
        if code_block_count % 2 != 0:
            result['errors'].append("Unbalanced code blocks (``` count is odd)")
            result['valid'] = False

        # Check for category headers
        category_headers = ['#### Entity Management', '#### Relationship Management', '#### Observation Management', '#### Graph Operations']
        category_count = sum(1 for header in category_headers if header in content)
        if category_count == 0:
            result['warnings'].append("No category headers found in tools section")

        result['info'].append(f"Document has {len(lines)} lines")

    def validate_content_integrity(self, content: str, result: Dict) -> None:
        """Validate content integrity and completeness."""

        # Check for required sections
        required_sections = [
            '## Table of Contents',
            '## Installation',
            '## Usage',
            '### Memory Tools',
            '### Memory Resources'
        ]

        for section in required_sections:
            if section not in content:
                result['warnings'].append(f"Missing section: {section}")

        # Check for tools content (handle both naming conventions)
        if 'memory.create_entities' not in content and 'memory_create_entities' not in content:
            result['errors'].append("Tools content appears to be missing or incomplete")
            result['valid'] = False

        # Check for resources content
        if 'memory://' not in content:
            result['errors'].append("Resources content appears to be missing or incomplete")
            result['valid'] = False

        # Check for proper parameter documentation
        parameter_patterns = ['Parameters:', 'Read-only:']
        for pattern in parameter_patterns:
            if pattern not in content:
                result['warnings'].append(f"Missing parameter documentation pattern: {pattern}")

    def validate_section_markers(self, content: str, result: Dict) -> None:
        """Validate section markers are properly formatted."""
        import re

        # Check for tools markers
        tools_start = 'Tools generated by' in content
        tools_end = '<!--- End of tools generated section -->' in content

        if not tools_start:
            result['errors'].append("Missing tools section start marker")
            result['valid'] = False
        elif not tools_end:
            result['errors'].append("Missing tools section end marker")
            result['valid'] = False
        else:
            result['info'].append("Tools section markers validated")

        # Check for resources markers
        resources_start = 'Resources generated by' in content
        resources_end = '<!--- End of resources generated section -->' in content

        if not resources_start:
            result['errors'].append("Missing resources section start marker")
            result['valid'] = False
        elif not resources_end:
            result['errors'].append("Missing resources section end marker")
            result['valid'] = False
        else:
            result['info'].append("Resources section markers validated")

    def validate_file_size(self, content: str, result: Dict) -> None:
        """Validate file size is reasonable."""
        size_bytes = len(content.encode('utf-8'))
        size_kb = size_bytes / 1024

        if size_bytes < 1000:
            result['warnings'].append(f"File seems very small: {size_kb:.1f} KB")
        elif size_bytes > 1024 * 1024:  # 1MB
            result['warnings'].append(f"File seems very large: {size_kb:.1f} KB")
        else:
            result['info'].append(f"File size OK: {size_kb:.1f} KB")

    def write_validation_report(self, validation_result: Dict) -> None:
        """Write validation report to staging directory."""
        report_path = self.staging_dir / "validation-report.txt"

        with open(report_path, 'w') as f:
            f.write("# Validation Report\n\n")
            f.write(f"Generated: {datetime.now().isoformat()}\n")
            f.write(f"Status: {'PASS' if validation_result['valid'] else 'FAIL'}\n\n")

            if validation_result['errors']:
                f.write("## Errors:\n")
                for error in validation_result['errors']:
                    f.write(f"- {error}\n")
                f.write("\n")

            if validation_result['warnings']:
                f.write("## Warnings:\n")
                for warning in validation_result['warnings']:
                    f.write(f"- {warning}\n")
                f.write("\n")

            if validation_result['info']:
                f.write("## Info:\n")
                for info in validation_result['info']:
                    f.write(f"- {info}\n")

        print(f"✓ Validation report written to {report_path}")

    def backup_readme(self) -> Optional[Path]:
        """Stage 5: Create backup of original README before applying changes."""
        if not self.readme_path.exists():
            print("ERROR: Original README.md not found")
            return None

        try:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            backup_filename = f"README-backup-{timestamp}.md"
            backup_path = self.staging_dir / backup_filename

            # Copy the original README
            import shutil
            shutil.copy2(self.readme_path, backup_path)

            return backup_path

        except Exception as e:
            print(f"ERROR: Failed to create backup: {e}")
            return None

    def apply_staging_changes(self, staging_path: Path) -> bool:
        """Stage 5: Apply the staging changes to the actual README."""
        try:
            if not staging_path.exists():
                print("ERROR: Staging file not found")
                return False

            # Read the staging content
            staging_content = staging_path.read_text()

            # Write to the actual README
            self.readme_path.write_text(staging_content)

            return True

        except Exception as e:
            print(f"ERROR: Failed to apply changes: {e}")
            return False

    def rollback_changes(self, backup_path: Path) -> bool:
        """Rollback changes by restoring from backup."""
        try:
            if not backup_path.exists():
                print("ERROR: Backup file not found")
                return False

            # Restore from backup
            import shutil
            shutil.copy2(backup_path, self.readme_path)
            print(f"✓ Successfully rolled back from {backup_path}")

            return True

        except Exception as e:
            print(f"ERROR: Failed to rollback: {e}")
            return False


def main():
    """Main entry point."""
    script_dir = Path(__file__).parent
    project_root = script_dir.parent

    generator = StagedDocumentationGenerator(project_root)
    success = generator.run()

    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())
