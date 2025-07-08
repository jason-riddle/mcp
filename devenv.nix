{ pkgs, ... }:
{
  # https://devenv.sh/integrations/dotenv/
  dotenv.enable = true;
  dotenv.filename = [
    ".env"
    ".env.local"
  ];

  # https://devenv.sh/languages/
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk17;
    maven = {
      enable = true;
      package = pkgs.maven;
    };
  };

  # https://devenv.sh/packages/
  packages = with pkgs; [
    flyctl
  ];

  # https://devenv.sh/git-hooks/
  # Organized by speed: fast checks first, slower ones last
  git-hooks.hooks = {
    # ========== FAST CHECKS (< 1s) ==========

    # Branch protection
    # no-commit-to-branch = {
    #   enable = true;
    #   args = [ "--branch" "main" "--branch" "master" ];
    # };

    # File format validation (fast)
    check-xml.enable = true;
    check-yaml.enable = true;
    check-json.enable = true;

    # File integrity checks (fast)
    check-merge-conflicts.enable = true;
    check-case-conflicts.enable = true;
    check-executables-have-shebangs.enable = true;
    check-shebang-scripts-are-executable.enable = true;
    check-added-large-files = {
      enable = true;
      args = [ "--maxkb=1024" ]; # 1MB limit for API projects
    };

    # File formatting (fast)
    end-of-file-fixer.enable = true;
    fix-byte-order-marker.enable = true;
    mixed-line-endings.enable = true;
    trim-trailing-whitespace = {
      enable = true;
      # args = [ "--markdown-linebreak-ext=md" ];
    };

    # ========== MEDIUM SPEED CHECKS (1-5s) ==========

    # Nix formatting
    nixfmt-rfc-style.enable = true;

    # YAML linting with configuration
    yamllint = {
      enable = true;
    };

    # Security validation
    ripsecrets = {
      enable = true;
    };

    # ========== SLOWER CHECKS (5-30s) ==========

    # Java code quality
    spotless-check = {
      enable = true;
      name = "spotless-check";
      entry = "./mvnw spotless:check";
      language = "system";
      files = "\\.(java|xml|json|yaml|yml|properties)$";
      pass_filenames = false;
    };

    # Maven checkstyle validation
    maven-checkstyle = {
      enable = true;
      name = "maven-checkstyle";
      entry = "./mvnw checkstyle:check";
      language = "system";
      files = "\\.java$";
      pass_filenames = false;
    };

    # Javadoc validation
    javadoc-check = {
      enable = true;
      name = "javadoc-check";
      entry = "./mvnw javadoc:javadoc -Dadditionalparam=-Xdoclint:all -Ddoclint=all,-missing";
      language = "system";
      files = "\\.java$";
      pass_filenames = false;
    };

    # Unit tests
    maven-test = {
      enable = true;
      name = "maven-test";
      entry = "./mvnw test";
      language = "system";
      files = "\\.(java|xml|properties)$";
      pass_filenames = false;
    };

    # SSE Integration tests
    # maven-sse-integration-test = {
    #   enable = true;
    #   name = "maven-sse-integration-test";
    #   entry = "./mvnw verify -Dtest=McpServerSseIntegrationTest";
    #   language = "system";
    #   files = "\\.(java|xml|properties)$";
    #   pass_filenames = false;
    # };

    # STDIO Integration tests
    # maven-stdio-integration-test = {
    #   enable = true;
    #   name = "maven-stdio-integration-test";
    #   entry = "./mvnw verify -Dtest=McpServerStdioIntegrationTest";
    #   language = "system";
    #   files = "\\.(java|xml|properties)$";
    #   pass_filenames = false;
    # };

    # Maven package
    # maven-package = {
    #   enable = true;
    #   name = "maven-package";
    #   entry = "./mvnw package";
    #   language = "system";
    #   files = "\\.(java|xml|properties)$";
    #   pass_filenames = false;
    # };

    # Dependency security check (only on pom.xml changes)
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

  # Environment variables for development
  env = {
    # Maven optimization
    MAVEN_OPTS = "-Xmx2g -XX:+UseG1GC";

    # Java development settings
    # JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8";
  };

  # Development shell setup
  enterShell = ''
    echo "🚀 Java Development Environment"
    echo "📦 Java: $(java -version 2>&1 | tail -n1)"
    echo "📦 Maven: $(mvn -version | head -n1)"
    echo "📍 Java path: $(which java)"
    echo "📍 Maven path: $(which mvn)"
    echo ""
    echo "💡 Tips:"
    echo "    - Use 'git commit --no-verify' to skip git hooks in emergencies"
    echo ""
  '';
}
