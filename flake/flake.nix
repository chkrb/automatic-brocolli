{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    systems.url = "github:nix-systems/default";
  };

  outputs =
    { nixpkgs, ... }@inputs:
    let
      eachSystem = nixpkgs.lib.genAttrs (import inputs.systems);
    in
    {
      devShells = eachSystem (
        system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config = {
              allowUnfree = true;
              android_sdk.accept_license = true;
            };
          };

          androidenvPlatformVersion = "37";
          androidenvBuildToolsVersion = "36.0.0";
          androidenvNdkVersion = "29.0.14206865";
          androidenv = pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ androidenvPlatformVersion ];
            buildToolsVersions = [ androidenvBuildToolsVersion ];
            ndkVersions = [ androidenvNdkVersion ];
            includeEmulator = false;
            includeNDK = true;
          };
        in
        with pkgs;
        {
          default = (
            mkShellNoCC rec {
              nativeBuildInputs = [
                androidenv.androidsdk
                jetbrains.jdk-no-jcef-21
              ];

              ANDROID_HOME = "${androidenv.androidsdk}/libexec/android-sdk";
              GRADLE_OPTS = ''
                -Dorg.gradle.configuration-cache=true
                -Dorg.gradle.project.android.aapt2FromMavenOverride=${ANDROID_HOME}/build-tools/${androidenvBuildToolsVersion}/aapt2
              '';

              packages = [
                git
                typst

                (callPackage ./packages/kotlin-lsp.nix { })

                (python3.withPackages (
                  py: with py; [
                    opencv4
                    pillow
                    pyside6
                    qrcode
                    zxing-cpp
                ]))
              ];
            }
          );
        }
      );
    };
}
