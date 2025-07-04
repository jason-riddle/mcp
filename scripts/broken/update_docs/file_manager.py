"""
File operations manager for safe README updates with backup and rollback.
"""

import re
import shutil
import tempfile
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from datetime import datetime

from .config import DocumentationConfig


class FileManager:
    """Manage file operations with backup and rollback capabilities."""

    def __init__(self, config: DocumentationConfig):
        """Initialize file manager with configuration."""
        self.config = config
        self.backup_dir: Optional[Path] = None
        self.staging_files: Dict[str, Path] = {}
        self.operations_log: List[str] = []

    def create_staging_environment(self) -> Path:
        """Create staging directory for safe operations."""
        if self.backup_dir is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            self.backup_dir = self.config.staging_dir / f"backup_{timestamp}"
            self.backup_dir.mkdir(parents=True, exist_ok=True)
            self.operations_log.append(f"Created staging directory: {self.backup_dir}")

        return self.backup_dir

    def create_backup(self, file_path: Path) -> Path:
        """Create backup of a file before modification."""
        if not file_path.exists():
            raise FileManagerError(f"Cannot backup non-existent file: {file_path}")

        staging_dir = self.create_staging_environment()
        backup_path = staging_dir / file_path.name

        shutil.copy2(file_path, backup_path)
        self.operations_log.append(f"Backed up {file_path} to {backup_path}")

        return backup_path

    def create_staging_file(self, original_path: Path, content: str) -> Path:
        """Create staging file with new content."""
        staging_dir = self.create_staging_environment()
        staging_path = staging_dir / f"{original_path.name}.staging"

        staging_path.write_text(content, encoding='utf-8')
        self.staging_files[str(original_path)] = staging_path
        self.operations_log.append(f"Created staging file: {staging_path}")

        return staging_path

    def update_readme_section(self, section_name: str, new_content: str) -> bool:
        """Update a specific section in README.md using markers."""
        readme_path = self.config.readme_path

        if not readme_path.exists():
            raise FileManagerError(f"README.md not found: {readme_path}")

        # Get section markers for this section
        if section_name not in self.config.section_markers:
            raise FileManagerError(f"Unknown section: {section_name}")

        markers = self.config.section_markers[section_name]
        start_pattern = markers.get('start_pattern', '')
        end_marker = markers.get('end_marker', '')
        new_start_marker = markers.get('new_start_marker', '')

        # Read current README content
        try:
            current_content = readme_path.read_text(encoding='utf-8')
        except Exception as e:
            raise FileManagerError(f"Failed to read README.md: {e}")

        # Create backup before any changes
        self.create_backup(readme_path)

        # Update the section
        updated_content = self._replace_section_content(
            current_content, start_pattern, end_marker, new_content, new_start_marker
        )

        # Create staging file with updated content
        staging_path = self.create_staging_file(readme_path, updated_content)

        self.operations_log.append(f"Updated {section_name} section in staging file")
        return True

    def _find_start_marker(self, content: str, start_pattern: str) -> Tuple[int, int, str]:
        """Find start marker using regex pattern and return position info."""
        match = re.search(start_pattern, content)
        if not match:
            raise FileManagerError(f"Start marker not found: {start_pattern}")

        start_pos = match.start()
        end_pos = match.end()
        actual_marker = match.group(0)
        return start_pos, end_pos, actual_marker

    def _replace_section_content(self, content: str, start_pattern: str, end_marker: str, new_content: str, new_start_marker: str) -> str:
        """Replace content between section markers using regex for start marker."""
        # Find start marker using regex
        start_pos, start_end_pos, actual_start_marker = self._find_start_marker(content, start_pattern)

        # Find end marker using exact string
        end_pos = content.find(end_marker)

        if end_pos == -1:
            raise FileManagerError(f"End marker not found: {end_marker}")

        if start_end_pos >= end_pos:
            raise FileManagerError("End marker appears before start marker")

        # Calculate positions - use the end of the matched start marker
        section_start = start_end_pos

        # Replace section content, including the new start marker
        before_section = content[:start_pos]
        after_section = content[end_pos:]

        # Build the new section with proper markers
        section_content = new_start_marker + '\n\n' + new_content

        # Ensure proper spacing
        if not before_section.endswith('\n'):
            before_section += '\n'

        if not section_content.endswith('\n'):
            section_content += '\n'

        if not after_section.startswith('\n'):
            section_content += '\n'

        return before_section + section_content + after_section

    def validate_staging_files(self) -> List[str]:
        """Validate all staging files before applying changes."""
        errors = []

        for original_path_str, staging_path in self.staging_files.items():
            original_path = Path(original_path_str)

            if not staging_path.exists():
                errors.append(f"Staging file missing: {staging_path}")
                continue

            try:
                # Basic validation - can read the file
                content = staging_path.read_text(encoding='utf-8')

                # Check file size is reasonable
                if len(content) == 0:
                    errors.append(f"Staging file is empty: {staging_path}")

                # Check for basic structure if it's README
                if original_path.name == "README.md":
                    if not self._validate_readme_structure(content):
                        errors.append(f"README structure validation failed: {staging_path}")

            except Exception as e:
                errors.append(f"Failed to validate staging file {staging_path}: {e}")

        return errors

    def _validate_readme_structure(self, content: str) -> bool:
        """Basic validation of README structure."""
        # Check for essential markers
        required_elements = [
            "# ",  # Has heading
            "##",  # Has subheadings
        ]

        for element in required_elements:
            if element not in content:
                return False

        # Check all configured section markers exist
        for section_name, markers in self.config.section_markers.items():
            start_pattern = markers.get('start_pattern', '')
            end_marker = markers.get('end_marker', '')
            if not re.search(start_pattern, content) or end_marker not in content:
                return False

        return True

    def apply_changes(self) -> bool:
        """Apply all staging files to their original locations."""
        validation_errors = self.validate_staging_files()
        if validation_errors:
            raise FileManagerError(f"Validation failed: {'; '.join(validation_errors)}")

        applied_files = []

        try:
            for original_path_str, staging_path in self.staging_files.items():
                original_path = Path(original_path_str)

                # Copy staging file to original location
                shutil.copy2(staging_path, original_path)
                applied_files.append(original_path)
                self.operations_log.append(f"Applied changes to: {original_path}")

            self.operations_log.append(f"Successfully applied changes to {len(applied_files)} files")
            return True

        except Exception as e:
            # If something went wrong, try to rollback the files we managed to change
            self._emergency_rollback(applied_files)
            raise FileManagerError(f"Failed to apply changes: {e}")

    def rollback_changes(self) -> bool:
        """Rollback all changes using backups."""
        if self.backup_dir is None or not self.backup_dir.exists():
            raise FileManagerError("No backup directory found for rollback")

        restored_files = []

        try:
            for original_path_str in self.staging_files.keys():
                original_path = Path(original_path_str)
                backup_path = self.backup_dir / original_path.name

                if backup_path.exists():
                    shutil.copy2(backup_path, original_path)
                    restored_files.append(original_path)
                    self.operations_log.append(f"Restored from backup: {original_path}")

            self.operations_log.append(f"Successfully rolled back {len(restored_files)} files")
            return True

        except Exception as e:
            raise FileManagerError(f"Failed to rollback changes: {e}")

    def _emergency_rollback(self, applied_files: List[Path]) -> None:
        """Emergency rollback for partially applied changes."""
        if self.backup_dir is None:
            return

        for original_path in applied_files:
            try:
                backup_path = self.backup_dir / original_path.name
                if backup_path.exists():
                    shutil.copy2(backup_path, original_path)
                    self.operations_log.append(f"Emergency rollback: {original_path}")
            except Exception as e:
                self.operations_log.append(f"Emergency rollback failed for {original_path}: {e}")

    def cleanup_staging(self, keep_backups: bool = True) -> None:
        """Clean up staging files and optionally keep backups."""
        if self.backup_dir and self.backup_dir.exists():
            if not keep_backups:
                shutil.rmtree(self.backup_dir)
                self.operations_log.append(f"Removed staging directory: {self.backup_dir}")
            else:
                # Only remove staging files, keep backups
                for staging_path in self.staging_files.values():
                    if staging_path.exists():
                        staging_path.unlink()
                        self.operations_log.append(f"Removed staging file: {staging_path}")

        self.staging_files.clear()

    def get_operations_log(self) -> List[str]:
        """Get log of all operations performed."""
        return self.operations_log.copy()

    def get_staging_preview(self, file_path: Path) -> Optional[str]:
        """Get preview of staging file content."""
        staging_path = self.staging_files.get(str(file_path))
        if staging_path and staging_path.exists():
            try:
                return staging_path.read_text(encoding='utf-8')
            except Exception:
                return None
        return None


class SectionManager:
    """Helper class for managing README sections."""

    def __init__(self, config: DocumentationConfig):
        """Initialize section manager with configuration."""
        self.config = config

    def extract_section_content(self, content: str, section_name: str) -> Optional[str]:
        """Extract content between section markers."""
        if section_name not in self.config.section_markers:
            return None

        markers = self.config.section_markers[section_name]
        start_pattern = markers.get('start_pattern', '')
        end_marker = markers.get('end_marker', '')

        try:
            start_pos, start_end_pos, actual_marker = self._find_start_marker(content, start_pattern)
        except FileManagerError:
            return None

        end_pos = content.find(end_marker)
        if end_pos == -1:
            return None

        section_start = start_end_pos
        return content[section_start:end_pos].strip()

    def validate_section_markers(self, content: str) -> Dict[str, List[str]]:
        """Validate all section markers in content."""
        results = {}

        for section_name, markers in self.config.section_markers.items():
            errors = []
            start_pattern = markers.get('start_pattern', '')
            end_marker = markers.get('end_marker', '')

            # Count matches using regex for start pattern
            start_matches = re.findall(start_pattern, content)
            start_count = len(start_matches)
            end_count = content.count(end_marker)

            if start_count == 0:
                errors.append(f"Start marker not found: {start_pattern}")
            elif start_count > 1:
                errors.append(f"Multiple start markers found: {start_count}")

            if end_count == 0:
                errors.append(f"End marker not found: {end_marker}")
            elif end_count > 1:
                errors.append(f"Multiple end markers found: {end_count}")

            if start_count == 1 and end_count == 1:
                try:
                    start_pos, start_end_pos, actual_marker = self._find_start_marker(content, start_pattern)
                    end_pos = content.find(end_marker)
                    if start_end_pos >= end_pos:
                        errors.append("End marker appears before start marker")
                except FileManagerError:
                    errors.append("Error validating marker positions")

            results[section_name] = errors

        return results

    def generate_section_markers(self, section_name: str) -> Tuple[str, str]:
        """Generate appropriate section markers for a section."""
        start_marker = f"<!-- START {section_name.upper()} -->"
        end_marker = f"<!-- END {section_name.upper()} -->"
        return start_marker, end_marker


class BackupManager:
    """Helper class for managing backups independently."""

    @staticmethod
    def create_timestamped_backup(file_path: Path, backup_dir: Optional[Path] = None) -> Path:
        """Create a timestamped backup of a file."""
        if not file_path.exists():
            raise FileManagerError(f"Cannot backup non-existent file: {file_path}")

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup_name = f"{file_path.stem}_{timestamp}{file_path.suffix}"

        if backup_dir is None:
            backup_dir = file_path.parent / "backups"

        backup_dir.mkdir(parents=True, exist_ok=True)
        backup_path = backup_dir / backup_name

        shutil.copy2(file_path, backup_path)
        return backup_path

    @staticmethod
    def restore_from_backup(backup_path: Path, target_path: Path) -> bool:
        """Restore file from backup."""
        if not backup_path.exists():
            raise FileManagerError(f"Backup file not found: {backup_path}")

        try:
            shutil.copy2(backup_path, target_path)
            return True
        except Exception as e:
            raise FileManagerError(f"Failed to restore from backup: {e}")

    @staticmethod
    def list_backups(file_path: Path, backup_dir: Optional[Path] = None) -> List[Path]:
        """List available backups for a file."""
        if backup_dir is None:
            backup_dir = file_path.parent / "backups"

        if not backup_dir.exists():
            return []

        pattern = f"{file_path.stem}_*{file_path.suffix}"
        return sorted(backup_dir.glob(pattern), reverse=True)  # Most recent first


class FileManagerError(Exception):
    """Custom exception for file manager operations."""
    pass
