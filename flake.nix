{
  description = "shen-truffle development environment";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/d407951447dcd00442e97087bf374aad70c04cea";
  outputs = { nixpkgs, ... }: let systems = [ "aarch64-darwin" "x86_64-darwin" "aarch64-linux" "x86_64-linux" ]; each = f: nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system}); in {
    packages = each (pkgs: let tools = [ pkgs.maven pkgs.graalvmPackages.graalvm-ce ]; in { toolchain = pkgs.buildEnv { name = "shen-truffle-toolchain"; paths = tools; }; default = pkgs.buildEnv { name = "shen-truffle-toolchain"; paths = tools; }; });
    devShells = each (pkgs: let graalvm = pkgs.graalvmPackages.graalvm-ce; in { default = pkgs.mkShell { packages = [ pkgs.maven graalvm ]; JAVA_HOME = "${graalvm}"; }; });
  };
}
