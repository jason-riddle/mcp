"""
Main orchestrator for the documentation generation system.

This module coordinates all components to extract data, generate markdown,
validate content, and update documentation files safely.
"""

import sys
from pathlib import Path
from typing import Dict, List

from .config import DocumentationConfig
from .extractors.java_extractor import JavaReflectionExtractor
from .extractors.regex_extractor import RegexDataExtractor
from .generators.tools_generator import ToolsGenerator
from .generators.resources_generator import ResourcesGenerator
from .generators.prompts_generator import PromptsGenerator
from .validators.markdown_validator import MarkdownValidator
from .file_manager import FileManager


class DocumentationOrchestrator:
    """Main orchestrator for the documentation generation pipeline."""

    def __init__(self, config: DocumentationConfig):
        """Initialize orchestrator with configuration."""
        self.config = config
        self.file_manager = FileManager(config)
        self.validator = MarkdownValidator()

        # Initialize extractors
        self.java_extractor = JavaReflectionExtractor(config, fallback_to_regex=True)
        self.regex_extractor = RegexDataExtractor(config)

        # Initialize generators
        self.tools_generator = ToolsGenerator(config)
        self.resources_generator = ResourcesGenerator(config)
        self.prompts_generator = PromptsGenerator(config)

    def run_full_pipeline(self, dry_run: bool = False) -> bool:
        """Run the complete documentation generation pipeline.

        Args:
            dry_run: If True, don't apply changes, just validate and report

        Returns:
            True if successful, False if there were errors
        """
        try:
            print("Starting documentation generation pipeline...")

            # Step 1: Validate configuration
            print("1. Validating configuration...")
            config_errors = self.config.validate()
            if config_errors:
                print(f"Configuration errors: {config_errors}")
                return False

            # Step 2: Extract data
            print("2. Extracting data from source files...")
            data = self._extract_data()
            if not data:
                print("No data extracted, aborting.")
                return False

            # Step 3: Generate markdown content
            print("3. Generating markdown content...")
            content = self._generate_content(data)

            # Step 4: Validate generated content
            print("4. Validating generated content...")
            validation_success = self._validate_content(content)
            if not validation_success:
                print("Content validation failed.")
                return False

            # Step 5: Apply changes (unless dry run)
            if dry_run:
                print("5. DRY RUN: Previewing changes...")
                self._preview_changes(content)
            else:
                print("5. Applying changes...")
                apply_success = self._apply_changes(content)
                if not apply_success:
                    print("Failed to apply changes.")
                    return False

            print("Documentation generation completed successfully!")
            return True

        except Exception as e:
            print(f"Pipeline failed with error: {e}")
            return False

    def _extract_data(self) -> Dict:
        """Extract tools, resources, and prompts data."""
        try:
            # Try Java extraction first, fall back to regex if needed
            if self.java_extractor.can_use_java_reflection():
                print("  Using Java reflection extraction...")
                data = self.java_extractor.extract_all()
            else:
                print("  Java reflection not available, using regex extraction...")
                data = self.regex_extractor.extract_all()

            print(f"  Extracted {len(data.get('tools', []))} tools")
            print(f"  Extracted {len(data.get('resources', []))} resources")
            print(f"  Extracted {len(data.get('prompts', []))} prompts")

            return data

        except Exception as e:
            print(f"  Data extraction failed: {e}")
            return {}

    def _generate_content(self, data: Dict) -> Dict[str, str]:
        """Generate markdown content for all sections."""
        content = {}

        try:
            # Generate tools section
            tools_data = data.get('tools', [])
            if tools_data:
                content['tools'] = self.tools_generator.generate_section(tools_data)
                print(f"  Generated tools section ({len(tools_data)} tools)")

            # Generate resources section
            resources_data = data.get('resources', [])
            if resources_data:
                content['resources'] = self.resources_generator.generate_section(resources_data)
                print(f"  Generated resources section ({len(resources_data)} resources)")

            # Generate prompts section
            prompts_data = data.get('prompts', [])
            if prompts_data:
                content['prompts'] = self.prompts_generator.generate_section(prompts_data)
                print(f"  Generated prompts section ({len(prompts_data)} prompts)")

        except Exception as e:
            print(f"  Content generation failed: {e}")

        return content

    def _validate_content(self, content: Dict[str, str]) -> bool:
        """Validate generated markdown content."""
        all_valid = True

        for section, markdown in content.items():
            print(f"  Validating {section} section...")
            result = self.validator.validate_markdown(markdown)

            if result.errors:
                print(f"    Errors in {section}: {result.errors}")
                all_valid = False

            if result.warnings:
                print(f"    Warnings in {section}: {result.warnings}")

            print(f"    {section}: {len(result.errors)} errors, {len(result.warnings)} warnings")

        return all_valid

    def _preview_changes(self, content: Dict[str, str]) -> None:
        """Preview what changes would be made."""
        print("  Changes that would be made:")

        for section, markdown in content.items():
            print(f"    - Update {section} section ({len(markdown.split('\n'))} lines)")

        # Create staging files for preview
        try:
            self.file_manager.create_staging_environment()
            for section, markdown in content.items():
                self.file_manager.update_readme_section(section, markdown)

            staging_readme = self.config.staging_dir / "README.md"
            if staging_readme.exists():
                preview = self.file_manager.get_staging_preview(staging_readme)
                if preview:
                    print(f"    - Staging README created ({len(preview.split('\n'))} lines)")
                else:
                    print("    - Staging README is empty")

        except Exception as e:
            print(f"    Preview generation failed: {e}")

    def _apply_changes(self, content: Dict[str, str]) -> bool:
        """Apply changes to documentation files."""
        try:
            # Create staging environment
            self.file_manager.create_staging_environment()

            # Update sections
            for section, markdown in content.items():
                self.file_manager.update_readme_section(section, markdown)

            # Validate staging files
            validation_success = self.file_manager.validate_staging_files()
            if not validation_success:
                print("  Staging validation failed, rolling back...")
                self.file_manager.rollback_changes()
                return False

            # Apply changes
            self.file_manager.apply_changes()
            print("  Changes applied successfully!")

            # Cleanup staging
            self.file_manager.cleanup_staging(keep_backups=True)

            return True

        except Exception as e:
            print(f"  Failed to apply changes: {e}")
            try:
                self.file_manager.rollback_changes()
            except:
                pass  # Rollback failure is not critical
            return False

    def update_single_section(self, section: str, dry_run: bool = False) -> bool:
        """Update a single documentation section.

        Args:
            section: Section to update ('tools', 'resources', or 'prompts')
            dry_run: If True, don't apply changes, just validate and report

        Returns:
            True if successful, False if there were errors
        """
        if section not in ['tools', 'resources', 'prompts']:
            print(f"Invalid section: {section}")
            return False

        try:
            print(f"Updating {section} section...")

            # Extract data
            data = self._extract_data()
            if not data:
                return False

            # Generate content for specific section
            section_data = data.get(section, [])
            if not section_data:
                print(f"No {section} data found.")
                return True

            if section == 'tools':
                markdown = self.tools_generator.generate_section(section_data)
            elif section == 'resources':
                markdown = self.resources_generator.generate_section(section_data)
            elif section == 'prompts':
                markdown = self.prompts_generator.generate_section(section_data)

            # Validate content
            result = self.validator.validate_markdown(markdown)
            if result.errors:
                print(f"Validation errors: {result.errors}")
                return False

            # Apply or preview changes
            if dry_run:
                print(f"DRY RUN: {section} section would be updated")
                print(f"Content preview (first 200 chars):")
                print(markdown[:200] + "..." if len(markdown) > 200 else markdown)
            else:
                self.file_manager.create_staging_environment()
                self.file_manager.update_readme_section(section, markdown)
                self.file_manager.apply_changes()
                self.file_manager.cleanup_staging(keep_backups=True)
                print(f"{section} section updated successfully!")

            return True

        except Exception as e:
            print(f"Failed to update {section} section: {e}")
            return False


def main():
    """Main entry point for the documentation generator."""
    import argparse

    parser = argparse.ArgumentParser(description="Generate MCP documentation")
    parser.add_argument("--project-root", type=Path, default=Path.cwd().parent,
                       help="Path to project root directory")
    parser.add_argument("--dry-run", action="store_true",
                       help="Preview changes without applying them")
    parser.add_argument("--section", choices=['tools', 'resources', 'prompts'],
                       help="Update only a specific section")
    parser.add_argument("--verbose", action="store_true",
                       help="Enable verbose output")

    args = parser.parse_args()

    try:
        # Initialize configuration
        config = DocumentationConfig.from_project_root(args.project_root)
        orchestrator = DocumentationOrchestrator(config)

        # Run requested operation
        if args.section:
            success = orchestrator.update_single_section(args.section, args.dry_run)
        else:
            success = orchestrator.run_full_pipeline(args.dry_run)

        sys.exit(0 if success else 1)

    except Exception as e:
        print(f"Error: {e}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
