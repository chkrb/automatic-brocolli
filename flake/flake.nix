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

          android-sdk = androidenv.androidsdk;
          jdk = pkgs.jetbrains.jdk-no-jcef-21;
          kotlin-lsp = (pkgs.callPackage ./packages/kotlin-lsp.nix { inherit jdk; });
        in
        with pkgs;
        rec {
          default = mkShellNoCC {
            packages = [
              git
              pipx
            ];
          };

          backend = mkShellNoCC {
            inputsFrom = [
              default
            ];

            buildInputs = [
              postgresql
              (python3.withPackages (
                py: with py; [
                  alembic
                  fastapi
                  psycopg
                  python-dotenv
                  sqlalchemy
                  uvicorn
                ]
              ))
            ];
          };

          customer-app = mkShellNoCC {
            inputsFrom = [
              default
            ];

            nativeBuildInputs = [
              android-sdk
              jdk
              kotlin-lsp
            ];

            ANDROID_HOME = "${androidenv.androidsdk}/libexec/android-sdk";
            GRADLE_OPTS = ''
              -Dorg.gradle.configuration-cache=true
              -Dorg.gradle.project.android.aapt2FromMavenOverride=${ANDROID_HOME}/build-tools/${androidenvBuildToolsVersion}/aapt2
            '';
          };

          idea = mkShellNoCC {
            inputsFrom = [
              default
            ];

            buildInputs = [
              typst
            ];
          };

          merchant-pos = mkShellNoCC {
            buildInputs = [
              (python3.withPackages (
                py: with py; [
                  opencv4
                  pillow
                  pyside6
                  qrcode
                  requests
                  zxing-cpp
                ]
              ))
            ];
          };

          merchant-pos-ng = mkShell {
            inputsFrom = [
              default
            ];

            nativeBuildInputs = [
              platformio
            ];
          };

          web = mkShellNoCC {
            inputsFrom = [
              default
            ];

            buildInputs = [
              nodejs
              vitejs
            ];
          };
        }
      );
    };
}
