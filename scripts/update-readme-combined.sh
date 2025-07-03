#!/bin/bash
#
# Copyright (c) 2025 Jason Riddle.
#
# Licensed under the MIT License.
#

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
README_PATH="$PROJECT_ROOT/README.md"

# Temporary files for JSON output
PYTHON_JSON=$(mktemp)
JAVA_JSON=$(mktemp)
COMBINED_JSON=$(mktemp)

# Cleanup function
cleanup() {
    rm -f "$PYTHON_JSON" "$JAVA_JSON" "$COMBINED_JSON"
}
trap cleanup EXIT

echo -e "${GREEN}Generating documentation using combined Python + Java approach...${NC}"

# Step 1: Generate Python JSON output (better resource descriptions)
echo -e "${YELLOW}Step 1: Extracting data using Python script...${NC}"
if ! python "$SCRIPT_DIR/update-readme.py" --json > "$PYTHON_JSON" 2>/dev/null; then
    echo -e "${RED}ERROR: Python script failed${NC}"
    exit 1
fi

# Step 2: Generate Java JSON output (better tool parameters)
echo -e "${YELLOW}Step 2: Extracting data using Java script...${NC}"
cd "$PROJECT_ROOT"
if ! make build >/dev/null 2>&1; then
    echo -e "${RED}ERROR: Build failed${NC}"
    exit 1
fi

if ! javac -cp "target/classes:target/quarkus-app/lib/main/*" scripts/UpdateReadme.java -d scripts/ 2>/dev/null; then
    echo -e "${RED}ERROR: Java compilation failed${NC}"
    exit 1
fi

if ! java -cp "scripts:target/classes:target/quarkus-app/lib/main/*" UpdateReadme --json > "$JAVA_JSON" 2>/dev/null; then
    echo -e "${RED}ERROR: Java script failed${NC}"
    exit 1
fi

# Step 3: Combine the best parts using jq
echo -e "${YELLOW}Step 3: Combining outputs to leverage strengths of both approaches...${NC}"
jq -n --slurpfile python "$PYTHON_JSON" --slurpfile java "$JAVA_JSON" '
{
  "tools": $java[0].tools,
  "resources": $python[0].resources
}' > "$COMBINED_JSON"

echo -e "${GREEN}Data extraction complete. Java tools: $(jq '.tools | length' "$COMBINED_JSON"), Python resources: $(jq '.resources | length' "$COMBINED_JSON")${NC}"

# Step 4: Generate README content from combined JSON
echo -e "${YELLOW}Step 4: Generating README content...${NC}"

generate_tools_section() {
    local tools_json="$1"

    echo "<details>"
    echo "<summary><b>Entity Management</b></summary>"
    echo ""

    # Entity Management tools
    jq -r '.tools[] | select(.name | contains("create_entities") or contains("delete_entities")) |
        "<!-- NOTE: This has been generated via update-readme-combined.sh -->\n\n" +
        "- **" + .name + "**\n" +
        "  - Title: " + .title + "\n" +
        "  - Description: " + .description + "\n" +
        (if (.parameters | length) > 0 then
            "  - Parameters:\n" +
            (.parameters | map("    - `" + .name + "` (" + .type + "): " + .description) | join("\n")) + "\n"
        else
            "  - Parameters: None\n"
        end) +
        "  - Read-only: **" + (.read_only | tostring) + "**\n"' "$tools_json"

    echo "</details>"
    echo ""

    echo "<details>"
    echo "<summary><b>Relationship Management</b></summary>"
    echo ""

    # Relationship Management tools
    jq -r '.tools[] | select(.name | contains("create_relations") or contains("delete_relations")) |
        "<!-- NOTE: This has been generated via update-readme-combined.sh -->\n\n" +
        "- **" + .name + "**\n" +
        "  - Title: " + .title + "\n" +
        "  - Description: " + .description + "\n" +
        (if (.parameters | length) > 0 then
            "  - Parameters:\n" +
            (.parameters | map("    - `" + .name + "` (" + .type + "): " + .description) | join("\n")) + "\n"
        else
            "  - Parameters: None\n"
        end) +
        "  - Read-only: **" + (.read_only | tostring) + "**\n"' "$tools_json"

    echo "</details>"
    echo ""

    echo "<details>"
    echo "<summary><b>Observation Management</b></summary>"
    echo ""

    # Observation Management tools
    jq -r '.tools[] | select(.name | contains("add_observations") or contains("delete_observations")) |
        "<!-- NOTE: This has been generated via update-readme-combined.sh -->\n\n" +
        "- **" + .name + "**\n" +
        "  - Title: " + .title + "\n" +
        "  - Description: " + .description + "\n" +
        (if (.parameters | length) > 0 then
            "  - Parameters:\n" +
            (.parameters | map("    - `" + .name + "` (" + .type + "): " + .description) | join("\n")) + "\n"
        else
            "  - Parameters: None\n"
        end) +
        "  - Read-only: **" + (.read_only | tostring) + "**\n"' "$tools_json"

    echo "</details>"
    echo ""

    echo "<details>"
    echo "<summary><b>Graph Operations</b></summary>"
    echo ""

    # Graph Operations tools
    jq -r '.tools[] | select(.name | contains("read_graph") or contains("search_nodes") or contains("open_nodes")) |
        "<!-- NOTE: This has been generated via update-readme-combined.sh -->\n\n" +
        "- **" + .name + "**\n" +
        "  - Title: " + .title + "\n" +
        "  - Description: " + .description + "\n" +
        (if (.parameters | length) > 0 then
            "  - Parameters:\n" +
            (.parameters | map("    - `" + .name + "` (" + .type + "): " + .description) | join("\n")) + "\n"
        else
            "  - Parameters: None\n"
        end) +
        "  - Read-only: **" + (.read_only | tostring) + "**\n"' "$tools_json"

    echo "</details>"
    echo ""
}

generate_resources_section() {
    local resources_json="$1"

    echo "<details>"
    echo "<summary><b>Memory Resources</b></summary>"
    echo ""

    jq -r '.resources[] |
        "<!-- NOTE: This has been generated via update-readme-combined.sh -->\n\n" +
        "- **" + .uri + "**\n" +
        "  - Title: " + .title + "\n" +
        "  - Description: " + .description + "\n"' "$resources_json"

    echo "</details>"
    echo ""
}

# Step 5: Update README.md
echo -e "${YELLOW}Step 5: Updating README.md...${NC}"

if [[ ! -f "$README_PATH" ]]; then
    echo -e "${RED}ERROR: README.md not found at $README_PATH${NC}"
    exit 1
fi

# Generate new content
TOOLS_CONTENT=$(generate_tools_section "$COMBINED_JSON")
RESOURCES_CONTENT=$(generate_resources_section "$COMBINED_JSON")

# Use Python to update the README (reusing the existing logic from Python script)
python3 -c "
import re

# Read README content
with open('$README_PATH', 'r') as f:
    content = f.read()

# Update tools section
tools_start_pattern = r'<!--- Tools generated by .* -->'
tools_end_marker = '<!--- End of tools generated section --->'
tools_new_marker = '<!--- Tools generated by update-readme-combined.sh --->'

start_match = re.search(tools_start_pattern, content)
if start_match:
    start_idx = start_match.end()
    end_idx = content.find(tools_end_marker)
    if end_idx != -1:
        content = (content[:start_match.start()] +
                  tools_new_marker + '\n\n' +
                  '''$TOOLS_CONTENT''' + '\n\n' +
                  content[end_idx:])
    else:
        print('WARNING: Tools end marker not found')
else:
    print('WARNING: Tools start marker not found')

# Update resources section
resources_start_pattern = r'<!--- Resources generated by .* -->'
resources_end_marker = '<!--- End of resources generated section --->'
resources_new_marker = '<!--- Resources generated by update-readme-combined.sh --->'

start_match = re.search(resources_start_pattern, content)
if start_match:
    start_idx = start_match.end()
    end_idx = content.find(resources_end_marker)
    if end_idx != -1:
        content = (content[:start_match.start()] +
                  resources_new_marker + '\n\n' +
                  '''$RESOURCES_CONTENT''' + '\n\n' +
                  content[end_idx:])
    else:
        print('WARNING: Resources end marker not found')
else:
    print('WARNING: Resources start marker not found')

# Write updated content
with open('$README_PATH', 'w') as f:
    f.write(content)
"

echo -e "${GREEN}README.md updated successfully using combined approach!${NC}"
echo -e "${GREEN}✓ Java script: Better parameter extraction for tools${NC}"
echo -e "${GREEN}✓ Python script: Better description extraction for resources${NC}"
