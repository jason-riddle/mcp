# GitHub Actions

This document contains detailed GitHub Actions workflow examples for Claude Code automation.

## Claude Code Actions Comparison

There are two Claude Code actions available, each designed for different use cases:

### `anthropics/claude-code-action@beta`
- **Purpose**: Interactive comment-based workflows
- **Supported Events**: `issue_comment`, `pull_request_review_comment`, `issues`, `pull_request`, `pull_request_review`
- **Use Case**: Manual triggering via comments (e.g., "@claude fix this") or automated triggers on PR/issue events
- **Features**: Trigger phrases, GitHub integration, sticky comments, assignee/label triggers
- **Limitations**: Requires GitHub app installation, limited to GitHub-specific workflows

### `anthropics/claude-code-base-action@beta`
- **Purpose**: Flexible automation for any GitHub event
- **Supported Events**: All GitHub events (`push`, `pull_request`, `issues`, `schedule`, etc.)
- **Use Case**: Automated workflows triggered by code changes, CI/CD pipelines
- **Features**: Direct prompts, system prompt customization, MCP support
- **Limitations**: No comment-based interaction features

## Parameter Differences

| Parameter               | `claude-code-action` | `claude-code-base-action` | Notes                                   |
|-------------------------|----------------------|---------------------------|-----------------------------------------|
| `anthropic_api_key`     | ✅                   | ✅                        | Required for both                       |
| `github_token`          | ✅                   | ❌                        | Auto-provided for comment action        |
| `trigger_phrase`        | ✅                   | ❌                        | Only for comment-based triggers         |
| `custom_instructions`   | ✅                   | ❌                        | Use `append_system_prompt` instead      |
| `direct_prompt`         | ✅                   | ❌                        | Use `prompt` instead                    |
| `prompt`                | ❌                   | ✅                        | Direct prompt for base action           |
| `append_system_prompt`  | ❌                   | ✅                        | Append to system prompt                 |
| `system_prompt`         | ❌                   | ✅                        | Override system prompt                  |
| `allowed_tools`         | ✅                   | ✅                        | Same format for both                    |
| `timeout_minutes`       | ✅                   | ✅                        | Same for both                           |
| `max_turns`             | ✅                   | ✅                        | Same for both                           |
| `mcp_config`            | ❌                   | ✅                        | MCP server configuration                |
| `claude_env`            | ❌                   | ✅                        | Custom environment variables            |

## Example Workflows

### 1. Automated Permutation Testing

The project uses `claude-code-base-action` for automated permutation testing analysis:

```yaml
name: Claude Smart Permutation Testing

on:
  push:
    branches: [main]
    paths:
      - 'src/**/*.java'
      - '!src/**/package-info.java'
      - '!src/**/*Test.java'
  issue_comment:
    types: [created]
  pull_request_review_comment:
    types: [created]

jobs:
  claude-permutation-analysis:
    runs-on: ubuntu-latest
    steps:
      - name: Claude Permutation Testing Analysis
        uses: anthropics/claude-code-base-action@beta
        with:
          anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}
          timeout_minutes: 4
          max_turns: "5"
          append_system_prompt: |
            You are an expert in permutation testing and software quality assurance.
            Analyze code changes and run appropriate JQwik permutation tests.
          prompt: |
            Analyze the changes in this commit and execute appropriate permutation tests.
            Focus on being smart about test selection.
          allowed_tools: |
            Edit,Read,Glob,Grep,LS,TodoWrite,Task
            Bash(./mvnw test -Dtest=MemoryGraphPermutationTest*)
            Bash(./mvnw test -Dtest=*PermutationTest*)
            Bash(git diff*),Bash(git log*),Bash(git show*)
```

### 2. Automated PR Code Review

Uses `claude-code-action` for interactive code review triggered by PR events:

```yaml
name: Claude Auto Review

on:
  pull_request:
    types: [opened, synchronize]

jobs:
  auto-review:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      pull-requests: read
      id-token: write
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 1

      - name: Automatic PR Review
        uses: anthropics/claude-code-action@beta
        with:
          anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}
          timeout_minutes: "60"
          direct_prompt: |
            Please review this pull request and provide comprehensive feedback.

            Focus on:
            - Code quality and best practices
            - Potential bugs or issues
            - Performance considerations
            - Security implications
            - Test coverage
            - Documentation updates if needed

            Provide constructive feedback with specific suggestions for improvement.
            Use inline comments to highlight specific areas of concern.
```

### 3. Automated Issue Triage

Uses `claude-code-base-action` for automated issue labeling and triage:

```yaml
name: Claude Issue Triage

on:
  issues:
    types: [opened]

jobs:
  triage-issue:
    runs-on: ubuntu-latest
    timeout-minutes: 10
    permissions:
      contents: read
      issues: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup GitHub MCP Server
        run: |
          mkdir -p /tmp/mcp-config
          cat > /tmp/mcp-config/mcp-servers.json << 'EOF'
          {
            "mcpServers": {
              "github": {
                "command": "docker",
                "args": [
                  "run",
                  "-i",
                  "--rm",
                  "-e",
                  "GITHUB_PERSONAL_ACCESS_TOKEN",
                  "ghcr.io/github/github-mcp-server:sha-7aced2b"
                ],
                "env": {
                  "GITHUB_PERSONAL_ACCESS_TOKEN": "${{ secrets.GITHUB_TOKEN }}"
                }
              }
            }
          }
          EOF

      - name: Run Claude Code for Issue Triage
        uses: anthropics/claude-code-base-action@beta
        with:
          prompt: |
            You're an issue triage assistant. Analyze the issue and apply appropriate labels.

            1. First, get available labels: `gh label list`
            2. Get issue details: use mcp__github__get_issue
            3. Analyze content and apply relevant labels
            4. Do not post comments, only apply labels
          allowed_tools: "Bash(gh label list),mcp__github__get_issue,mcp__github__get_issue_comments,mcp__github__update_issue,mcp__github__search_issues,mcp__github__list_issues"
          mcp_config: /tmp/mcp-config/mcp-servers.json
          timeout_minutes: "5"
          anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}
```

## Migration Guide

When switching from `claude-code-action` to `claude-code-base-action`:

### 1. Update Action Reference
```yaml
# Old
uses: anthropics/claude-code-action@beta

# New
uses: anthropics/claude-code-base-action@beta
```

### 2. Update Parameters
```yaml
# Old
custom_instructions: |
  System prompt content
direct_prompt: |
  Direct prompt content
github_token: ${{ secrets.GITHUB_TOKEN }}
trigger_phrase: "@claude-test"

# New
append_system_prompt: |
  System prompt content
prompt: |
  Direct prompt content
# Remove github_token and trigger_phrase
```

### 3. Update Event Triggers
```yaml
# For push events, use base action
on:
  push:
    branches: [main]

# For comment events, either action works
on:
  issue_comment:
    types: [created]
```

## Best Practices

### 1. Choose the Right Action
- Use `claude-code-action` for manual, comment-triggered workflows
- Use `claude-code-base-action` for automated, event-triggered workflows

### 2. Security Configuration
- Always use `${{ secrets.ANTHROPIC_API_KEY }}` for API keys
- Limit `allowed_tools` to minimum required permissions
- Set appropriate `timeout_minutes` to prevent runaway executions

### 3. Tool Restrictions
- Use specific patterns for Bash tools (e.g., `Bash(./mvnw test -Dtest=*Test*)`)
- Avoid overly broad permissions like `Bash(*)`
- Test tool permissions in development environment first

### 4. Error Handling
- Monitor workflow runs for timeout or permission errors
- Use `max_turns` to prevent infinite loops
- Implement proper cleanup in post-actions

## Common Issues

### "Unsupported event type: push"
- **Cause**: Using `claude-code-action` with push events
- **Solution**: Switch to `claude-code-base-action`

### Parameter Not Recognized
- **Cause**: Using wrong parameter names between actions
- **Solution**: Use parameter mapping table above

### Tool Permission Denied
- **Cause**: Tool not listed in `allowed_tools`
- **Solution**: Add specific tool pattern to `allowed_tools`

### "Unexpected input(s) 'additional_permissions'"
- **Cause**: Using `additional_permissions` parameter with `claude-code-base-action@beta`
- **Solution**: Remove the `additional_permissions` parameter - it's not supported by the base action
- **Context**: This parameter was valid in earlier versions but is no longer supported
