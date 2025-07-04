#!/usr/bin/env python3
"""
MCP Documentation Generator - Wrapper Script

This script provides a simple interface to the new modular documentation
generation system. It replaces the old monolithic update-docs.py with
a clean, maintainable wrapper around the new architecture.

Usage:
    python update-docs.py                    # Full pipeline
    python update-docs.py --dry-run          # Preview changes
    python update-docs.py --section tools    # Update only tools
    python update-docs.py --help             # Show help

The new system features:
- Modular architecture with clear separation of concerns
- Comprehensive test coverage (142+ tests)
- Safe file operations with backup and rollback
- Java reflection with regex fallback for data extraction
- Markdown validation and content enhancement
- Integration tests for end-to-end workflow validation
"""

import sys
from pathlib import Path

# Add the update_docs package to Python path
SCRIPT_DIR = Path(__file__).parent
sys.path.insert(0, str(SCRIPT_DIR))

from update_docs.main import main


if __name__ == "__main__":
    # Simple wrapper that delegates to the main orchestrator
    try:
        main()
    except KeyboardInterrupt:
        print("\nOperation cancelled by user.")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)
