{
  description = "shen-truffle development environment";
  # This revision provides GraalVM CE 25.2.4, matching pom.xml's Truffle line.
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/ac6b2166e7a9375683b8e98f860f273222337b16";
  outputs = { nixpkgs, ... }: let systems = [ "aarch64-darwin" "x86_64-darwin" "aarch64-linux" "x86_64-linux" ]; each = f: nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system}); in {
    packages = each (pkgs: let tools = [ pkgs.maven pkgs.graalvmPackages.graalvm-ce ]; in { toolchain = pkgs.buildEnv { name = "shen-truffle-toolchain"; paths = tools; }; default = pkgs.buildEnv { name = "shen-truffle-toolchain"; paths = tools; }; });
    devShells = each (pkgs: let graalvm = pkgs.graalvmPackages.graalvm-ce; in { default = pkgs.mkShell { packages = [ pkgs.maven graalvm ]; JAVA_HOME = "${graalvm}"; }; });
  };
}
