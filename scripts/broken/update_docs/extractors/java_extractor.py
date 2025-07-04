"""
Java reflection-based data extraction.
"""

import json
import subprocess
from typing import Dict, List
from pathlib import Path

from .base import DataExtractor, ParameterInfo, ToolInfo, ResourceInfo, ExtractionError
from .regex_extractor import RegexDataExtractor


class JavaReflectionExtractor(DataExtractor):
    """Extract MCP tools and resources using Java reflection via subprocess."""

    def __init__(self, config, fallback_to_regex: bool = True):
        """Initialize Java extractor with optional regex fallback."""
        super().__init__(config)
        self.fallback_to_regex = fallback_to_regex
        self._regex_extractor = None

    @property
    def regex_extractor(self) -> RegexDataExtractor:
        """Lazy initialization of regex extractor for fallback."""
        if self._regex_extractor is None:
            self._regex_extractor = RegexDataExtractor(self.config)
        return self._regex_extractor

    def extract_tools(self) -> List[Dict]:
        """Extract tools using Java reflection with regex fallback."""
        try:
            return self._extract_with_java_reflection()['tools']
        except Exception as e:
            if self.fallback_to_regex:
                print(f"Java reflection failed ({e}), falling back to regex extraction")
                return self.regex_extractor.extract_tools()
            else:
                raise ExtractionError(f"Java reflection extraction failed: {e}")

    def extract_resources(self) -> List[Dict]:
        """Extract resources using Java reflection with regex fallback."""
        try:
            return self._extract_with_java_reflection()['resources']
        except Exception as e:
            if self.fallback_to_regex:
                print(f"Java reflection failed ({e}), falling back to regex extraction")
                return self.regex_extractor.extract_resources()
            else:
                raise ExtractionError(f"Java reflection extraction failed: {e}")

    def extract_prompts(self) -> List[Dict]:
        """Extract prompts using Java reflection with regex fallback."""
        try:
            result = self._extract_with_java_reflection()
            return result.get('prompts', [])
        except Exception as e:
            if self.fallback_to_regex:
                print(f"Java reflection failed ({e}), falling back to regex extraction")
                return self.regex_extractor.extract_prompts()
            else:
                raise ExtractionError(f"Java reflection extraction failed: {e}")

    def extract_all(self) -> Dict:
        """Extract both tools and resources using Java reflection with regex fallback."""
        try:
            return self._extract_with_java_reflection()
        except Exception as e:
            if self.fallback_to_regex:
                print(f"Java reflection failed ({e}), falling back to regex extraction")
                return self.regex_extractor.extract_all()
            else:
                raise ExtractionError(f"Java reflection extraction failed: {e}")

    def _extract_with_java_reflection(self) -> Dict:
        """Execute Java reflection extraction via subprocess."""
        # Build Java command
        java_cmd = self._build_java_command()

        try:
            # Execute Java subprocess
            result = subprocess.run(
                java_cmd,
                capture_output=True,
                text=True,
                cwd=self.config.project_root,
                timeout=30  # 30 second timeout
            )

            if result.returncode != 0:
                raise ExtractionError(f"Java process failed (exit code {result.returncode}): {result.stderr}")

            # Parse JSON output
            if not result.stdout.strip():
                raise ExtractionError("Java process returned empty output")

            try:
                data = json.loads(result.stdout)
            except json.JSONDecodeError as e:
                raise ExtractionError(f"Failed to parse Java output as JSON: {e}")

            # Validate expected structure (tools and resources required, prompts optional)
            if not isinstance(data, dict) or 'tools' not in data or 'resources' not in data:
                raise ExtractionError("Java output missing expected 'tools' and 'resources' keys")

            # Process and validate the data
            return self._process_java_output(data)

        except subprocess.TimeoutExpired:
            raise ExtractionError("Java reflection process timed out after 30 seconds")
        except subprocess.SubprocessError as e:
            raise ExtractionError(f"Failed to execute Java process: {e}")

    def _build_java_command(self) -> List[str]:
        """Build the Java command with proper classpath."""
        # Base classpath components
        classpath_parts = [
            "scripts",
            "target/classes"
        ]

        # Add Quarkus dependencies if they exist
        quarkus_lib_dir = self.config.project_root / "target" / "quarkus-app" / "lib" / "main"
        if quarkus_lib_dir.exists():
            classpath_parts.append("target/quarkus-app/lib/main/*")

        # Build full classpath
        classpath = ":".join(classpath_parts)

        # Build command
        java_cmd = [
            "java",
            "-cp", classpath,
            "UpdateReadme",
            "--json"
        ]

        return java_cmd

    def _process_java_output(self, data: Dict) -> Dict:
        """Process and validate Java reflection output."""
        processed_tools = []
        processed_resources = []

        # Process tools
        for tool in data.get('tools', []):
            try:
                processed_tool = self._process_tool_data(tool)
                processed_tools.append(processed_tool)
            except Exception as e:
                print(f"Warning: Failed to process tool {tool.get('name', 'unknown')}: {e}")
                continue

        # Process resources
        for resource in data.get('resources', []):
            try:
                processed_resource = self._process_resource_data(resource)
                processed_resources.append(processed_resource)
            except Exception as e:
                print(f"Warning: Failed to process resource {resource.get('uri', 'unknown')}: {e}")
                continue

        return {
            'tools': processed_tools,
            'resources': processed_resources
        }

    def _process_tool_data(self, tool: Dict) -> Dict:
        """Process individual tool data from Java reflection."""
        # Validate required fields
        required_fields = ['name', 'description', 'method_name', 'return_type']
        for field in required_fields:
            if field not in tool:
                raise ValueError(f"Missing required field: {field}")

        # Process parameters
        parameters = []
        for param in tool.get('parameters', []):
            param_info = ParameterInfo(
                name=param['name'],
                param_type=self.config.simplify_java_type(param['type']),
                description=param['description'],
                required=param.get('required', True)
            )
            parameters.append(param_info)

        # Create ToolInfo and convert to dict
        tool_info = ToolInfo(
            name=tool['name'],
            description=tool['description'],
            method_name=tool['method_name'],
            return_type=tool['return_type'],
            parameters=parameters
        )

        return tool_info.to_dict(self.config)

    def _process_resource_data(self, resource: Dict) -> Dict:
        """Process individual resource data from Java reflection."""
        # Validate required fields
        required_fields = ['uri', 'method_name']
        for field in required_fields:
            if field not in resource:
                raise ValueError(f"Missing required field: {field}")

        # Create ResourceInfo and convert to dict
        resource_info = ResourceInfo(
            uri=resource['uri'],
            method_name=resource['method_name'],
            description=resource.get('description', '')
        )

        return resource_info.to_dict()

    def check_java_availability(self) -> Dict[str, bool]:
        """Check if Java reflection extraction prerequisites are available."""
        checks = {
            'java_available': False,
            'classes_compiled': False,
            'update_readme_exists': False,
            'tools_file_exists': False,
            'resources_file_exists': False
        }

        # Check Java availability
        try:
            result = subprocess.run(['java', '-version'], capture_output=True, timeout=5)
            checks['java_available'] = result.returncode == 0
        except (subprocess.SubprocessError, FileNotFoundError):
            pass

        # Check compiled classes
        target_classes = self.config.project_root / "target" / "classes"
        checks['classes_compiled'] = target_classes.exists()

        # Check UpdateReadme class (would need to be created)
        update_readme_class = self.config.project_root / "scripts" / "UpdateReadme.java"
        checks['update_readme_exists'] = update_readme_class.exists()

        # Check source files
        checks['tools_file_exists'] = self.config.get_tools_file_path().exists()
        checks['resources_file_exists'] = self.config.get_resources_file_path().exists()

        return checks

    def can_use_java_reflection(self) -> bool:
        """Check if Java reflection can be used."""
        checks = self.check_java_availability()
        return all([
            checks['java_available'],
            checks['classes_compiled'],
            checks['tools_file_exists'],
            checks['resources_file_exists']
        ])


class JavaClasspathBuilder:
    """Helper class for building Java classpaths."""

    def __init__(self, project_root: Path):
        self.project_root = project_root

    def build_classpath(self) -> str:
        """Build complete classpath for Java reflection."""
        parts = []

        # Add scripts directory (for UpdateReadme class)
        scripts_dir = self.project_root / "scripts"
        if scripts_dir.exists():
            parts.append(str(scripts_dir))

        # Add compiled classes
        target_classes = self.project_root / "target" / "classes"
        if target_classes.exists():
            parts.append(str(target_classes))

        # Add Quarkus lib directory
        quarkus_lib = self.project_root / "target" / "quarkus-app" / "lib" / "main"
        if quarkus_lib.exists():
            parts.append(str(quarkus_lib / "*"))

        # Add Maven dependencies if available
        maven_deps = self.project_root / "target" / "dependency"
        if maven_deps.exists():
            parts.append(str(maven_deps / "*"))

        return ":".join(parts)

    def find_jar_files(self) -> List[Path]:
        """Find all JAR files in the project."""
        jar_files = []

        # Look in target/lib
        lib_dir = self.project_root / "target" / "lib"
        if lib_dir.exists():
            jar_files.extend(lib_dir.glob("*.jar"))

        # Look in target/quarkus-app/lib
        quarkus_lib = self.project_root / "target" / "quarkus-app" / "lib"
        if quarkus_lib.exists():
            jar_files.extend(quarkus_lib.rglob("*.jar"))

        return jar_files


class JavaProcessManager:
    """Helper class for managing Java subprocess execution."""

    @staticmethod
    def execute_java_command(cmd: List[str], cwd: Path, timeout: int = 30) -> subprocess.CompletedProcess:
        """Execute Java command with proper error handling."""
        try:
            return subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                cwd=cwd,
                timeout=timeout,
                check=False  # Don't raise on non-zero exit
            )
        except subprocess.TimeoutExpired as e:
            raise ExtractionError(f"Java process timed out after {timeout} seconds")
        except subprocess.SubprocessError as e:
            raise ExtractionError(f"Failed to execute Java process: {e}")

    @staticmethod
    def validate_java_output(output: str) -> Dict:
        """Validate and parse Java process output."""
        if not output.strip():
            raise ExtractionError("Java process returned empty output")

        try:
            data = json.loads(output)
        except json.JSONDecodeError as e:
            raise ExtractionError(f"Failed to parse Java output as JSON: {e}")

        # Validate structure
        if not isinstance(data, dict):
            raise ExtractionError("Java output is not a JSON object")

        if 'tools' not in data or 'resources' not in data:
            raise ExtractionError("Java output missing required 'tools' and 'resources' keys")

        return data
