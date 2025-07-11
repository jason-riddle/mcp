{ pkgs, ... }:
{
  # ============================================================================
  # ENVIRONMENT CONFIGURATION
  # ============================================================================

  # https://devenv.sh/integrations/dotenv/
  dotenv = {
    enable = true;
    filename = [
      ".env"
      ".env.local"
    ];
  };

  # ============================================================================
  # LANGUAGE & RUNTIME CONFIGURATION
  # ============================================================================

  # https://devenv.sh/languages/
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk17;
    maven = {
      enable = true;
      package = pkgs.maven;
    };
  };

  # ============================================================================
  # PACKAGES & TOOLS
  # ============================================================================

  # https://devenv.sh/packages/
  packages = with pkgs; [
    flyctl
  ];

  # ============================================================================
  # GIT HOOKS CONFIGURATION
  # ============================================================================
  # https://devenv.sh/git-hooks/
  # Organized by execution speed: fast checks first, slower ones last

  git-hooks.hooks = {

    # ==========================================================================
    # FAST CHECKS (< 1s)
    # ==========================================================================

    # --------------------------------------------------------------------------
    # File Format Validation
    # --------------------------------------------------------------------------
    check-xml.enable = true;
    check-yaml.enable = true;
    check-json.enable = true;

    # --------------------------------------------------------------------------
    # File Integrity Checks
    # --------------------------------------------------------------------------
    check-merge-conflicts.enable = true;
    check-case-conflicts.enable = true;
    check-executables-have-shebangs.enable = true;
    check-shebang-scripts-are-executable.enable = true;

    check-added-large-files = {
      enable = true;
      args = [ "--maxkb=1024" ]; # 1MB limit for API projects
    };

    # --------------------------------------------------------------------------
    # File Formatting
    # --------------------------------------------------------------------------
    end-of-file-fixer.enable = true;
    fix-byte-order-marker.enable = true;
    mixed-line-endings.enable = true;

    trim-trailing-whitespace = {
      enable = true;
      # args = [ "--markdown-linebreak-ext=md" ];
    };

    # --------------------------------------------------------------------------
    # Branch Protection (commented out for flexibility)
    # --------------------------------------------------------------------------
    # no-commit-to-branch = {
    #   enable = true;
    #   args = [ "--branch" "main" "--branch" "master" ];
    # };

    # ==========================================================================
    # MEDIUM SPEED CHECKS (1-5s)
    # ==========================================================================

    # --------------------------------------------------------------------------
    # Code Formatting
    # --------------------------------------------------------------------------
    nixfmt-rfc-style.enable = true;

    # --------------------------------------------------------------------------
    # Configuration Linting
    # --------------------------------------------------------------------------
    yamllint = {
      enable = true;
    };

    # ==========================================================================
    # SECURITY VALIDATION
    # ==========================================================================

    # --------------------------------------------------------------------------
    # Secrets Detection (Multiple Layers)
    # --------------------------------------------------------------------------

    # Fast regex-based secret detection
    ripsecrets = {
      enable = true;
    };

    # SOPS encryption enforcement
    pre-commit-hook-ensure-sops = {
      enable = true;
      # This hook checks that specified files are encrypted with SOPS
      # It automatically works with your .sops.yaml configuration
    };

    # Comprehensive secrets scanner (slower but thorough)
    trufflehog = {
      enable = true;
      # Scan entire repository including git history for secrets
      # More thorough than ripsecrets but slower
      pass_filenames = false;
      # Only run on manual invocation or when files change
      stages = [
        "pre-commit"
        "manual"
      ];
    };

    # --------------------------------------------------------------------------
    # Credential Detection
    # --------------------------------------------------------------------------

    # AWS credentials detection
    detect-aws-credentials = {
      enable = true;
      # Allow missing credentials for local dev environments
      args = [ "--allow-missing-credentials" ];
    };

    # Private key detection
    detect-private-keys = {
      enable = true;
      # Critical for MCP servers that may handle authentication
    };

    # --------------------------------------------------------------------------
    # Documentation Security
    # --------------------------------------------------------------------------

    # VCS permalink validation
    check-vcs-permalinks = {
      enable = true;
      # Ensure documentation links are permanent (not subject to branch changes)
    };

    # ==========================================================================
    # SLOWER CHECKS (5-30s)
    # ==========================================================================

    # --------------------------------------------------------------------------
    # Java Code Quality
    # --------------------------------------------------------------------------

    # Code formatting validation
    spotless-check = {
      enable = true;
      name = "spotless-check";
      entry = "make format-check";
      language = "system";
      files = "\\.(java|xml|json|yaml|yml|properties)$";
      pass_filenames = false;
    };

    # Style guide validation
    maven-checkstyle = {
      enable = true;
      name = "maven-checkstyle";
      entry = "make checkstyle";
      language = "system";
      files = "\\.java$";
      pass_filenames = false;
    };

    # Cognitive complexity validation
    pmd-cognitive-complexity = {
      enable = true;
      name = "pmd-cognitive-complexity";
      entry = "./mvnw pmd:check";
      language = "system";
      files = "\\.java$";
      pass_filenames = false;
    };

    # Documentation validation
    javadoc-check = {
      enable = true;
      name = "javadoc-check";
      entry = "./mvnw javadoc:javadoc -Dadditionalparam=-Xdoclint:all -Ddoclint=all,-missing";
      language = "system";
      files = "\\.java$";
      pass_filenames = false;
    };

    # --------------------------------------------------------------------------
    # Testing
    # --------------------------------------------------------------------------

    # Unit tests
    maven-test = {
      enable = true;
      name = "maven-test";
      entry = "make test";
      language = "system";
      files = "\\.(java|xml|properties)$";
      pass_filenames = false;
    };

    # --------------------------------------------------------------------------
    # Optional/Disabled Checks
    # --------------------------------------------------------------------------

    # SSE Integration tests (disabled for performance)
    # maven-sse-integration-test = {
    #   enable = true;
    #   name = "maven-sse-integration-test";
    #   entry = "./mvnw verify -Dtest=McpServerSseIntegrationTest";
    #   language = "system";
    #   files = "\\.(java|xml|properties)$";
    #   pass_filenames = false;
    # };

    # STDIO Integration tests (disabled for performance)
    # maven-stdio-integration-test = {
    #   enable = true;
    #   name = "maven-stdio-integration-test";
    #   entry = "./mvnw verify -Dtest=McpServerStdioIntegrationTest";
    #   language = "system";
    #   files = "\\.(java|xml|properties)$";
    #   pass_filenames = false;
    # };

    # Maven package (disabled for performance)
    # maven-package = {
    #   enable = true;
    #   name = "maven-package";
    #   entry = "make build";
    #   language = "system";
    #   files = "\\.(java|xml|properties)$";
    #   pass_filenames = false;
    # };

    # Dependency security check (disabled - only on pom.xml changes)
    # dependency-check = {
    #   enable = true;
    #   name = "dependency-check";
    #   entry = "${pkgs.writeShellScript "dependency-check" ''
    #     # Check if dependency-check plugin is configured
    #     if grep -q "dependency-check-maven" pom.xml 2>/dev/null; then
    #       ./mvnw dependency-check:check -DfailBuildOnCVSS=7
    #     else
    #       echo "⚠️  dependency-check-maven plugin not configured in pom.xml"
    #       exit 0  # Don't fail if plugin isn't configured
    #     fi
    #   ''}";
    #   language = "system";
    #   files = "pom\\.xml$";
    #   pass_filenames = false;
    # };
  };

  # ============================================================================
  # ENVIRONMENT VARIABLES
  # ============================================================================

  env = {
    # Maven optimization for development
    MAVEN_OPTS = "-Xmx2g -XX:+UseG1GC";

    # Java development settings (commented out to avoid conflicts)
    # JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8";
  };

  # ============================================================================
  # DEVELOPMENT SHELL SETUP
  # ============================================================================

  enterShell = ''
    echo "🚀 Java MCP Server Development Environment"
    echo ""
    echo "📦 Runtime Information:"
    echo "   Java: $(java -version 2>&1 | tail -n1)"
    echo "   Maven: $(mvn -version | head -n1)"
    echo ""
    echo "📍 Tool Locations:"
    echo "   Java: $(which java)"
    echo "   Maven: $(which mvn)"
    echo ""
    echo "💡 Development Tips:"
    echo "   - Use 'git commit --no-verify' to skip git hooks in emergencies"
    echo "   - Use 'make env-encrypt' to encrypt environment files"
    echo "   - Use 'make env-decrypt' to decrypt environment files"
    echo ""
  '';
}
