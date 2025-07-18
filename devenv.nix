{ pkgs, config, ... }:
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
  # GIT HOOKS CONFIGURATION
  # ============================================================================
  # https://devenv.sh/git-hooks/
  # Organized by execution speed: fast checks first, slower ones last

  git-hooks.hooks = {

    # ==========================================================================
    # FAST VALIDATION (< 1s)
    # ==========================================================================

    # File format and integrity checks
    check-xml.enable = true;
    check-yaml.enable = true;
    check-merge-conflicts.enable = true;
    check-case-conflicts.enable = true;
    check-shebang-scripts-are-executable.enable = true;

    check-added-large-files = {
      enable = true;
      args = [ "--maxkb=1024" ]; # 1MB limit for API projects
    };

    # File formatting fixes
    end-of-file-fixer.enable = true;
    fix-byte-order-marker.enable = true;
    mixed-line-endings.enable = true;
    trim-trailing-whitespace.enable = true;

    # ==========================================================================
    # CODE FORMATTING (1-5s)
    # ==========================================================================

    # NIX
    nixfmt-rfc-style.enable = true;

    # YAML
    yamllint.enable = true;

    # ==========================================================================
    # SECURITY VALIDATION
    # ==========================================================================

    # Fast regex-based secret detection
    ripsecrets = {
      enable = true;
    };

    # SOPS encryption enforcement
    pre-commit-hook-ensure-sops = {
      enable = true;
    };

    # Comprehensive secrets scanner (slower but thorough)
    trufflehog = {
      enable = true;
      pass_filenames = false;
      stages = [
        "pre-commit"
        "manual"
      ];
    };

    # AWS credentials detection
    detect-aws-credentials = {
      enable = true;
      args = [ "--allow-missing-credentials" ];
    };

    # Private key detection
    detect-private-keys = {
      enable = true;
    };

    # VCS permalink validation
    check-vcs-permalinks = {
      enable = true;
    };
  };
}
