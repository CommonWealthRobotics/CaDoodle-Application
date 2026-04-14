{
  description = "CaDoodle development shell";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        jdk = pkgs.zulu21;
        openjfx = pkgs.openjfx;
        gradleJavafxInit = pkgs.writeText "cadoodle-javafx-init.gradle" ''
          allprojects {
            plugins.withId('java') {
              dependencies {
                implementation files(
                  '${openjfx}/modules/javafx.base',
                  '${openjfx}/modules/javafx.controls',
                  '${openjfx}/modules/javafx.fxml',
                  '${openjfx}/modules/javafx.graphics',
                  '${openjfx}/modules/javafx.media',
                  '${openjfx}/modules/javafx.swing',
                  '${openjfx}/modules/javafx.web'
                )
              }
            }
          }
        '';
        javafxNativeLibs = [
          "${openjfx}/modules_libs/javafx.base"
          "${openjfx}/modules_libs/javafx.graphics"
          "${openjfx}/modules_libs/javafx.media"
        ];
        runtimeLibs = with pkgs; [
          alsa-lib
          ffmpeg
          gtk2
          gtk3
          libGL
          libx11
          libxext
          libxi
          libxrandr
          libxrender
          libxtst
          libxxf86vm
          pango
        ];
      in {
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            jdk
            git
            openjfx
            wget
            zip
            xorg-server
          ] ++ runtimeLibs;

          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath runtimeLibs
            + ":"
            + pkgs.lib.concatStringsSep ":" javafxNativeLibs;

          shellHook = ''
            export JAVA_HOME="${jdk}"
            export PATH="$JAVA_HOME/bin:$PATH"
            export GRADLE_USER_HOME="$PWD/.gradle-nix"
            mkdir -p "$GRADLE_USER_HOME/init.d"
            ln -sfn "${gradleJavafxInit}" "$GRADLE_USER_HOME/init.d/cadoodle-javafx-init.gradle"

            echo "CaDoodle dev shell ready"
            echo "Run: git submodule update --init --recursive"
            echo "Then: ./gradlew run"
          '';
        };
      });
}
