{
  autoPatchelfHook,
  jdk,
  makeWrapper,
  stdenv,
  stdenvNoCC,
}:

stdenvNoCC.mkDerivation (finalAttrs: {
  pname = "kotlin-lsp";
  version = "263.4421.0";

  src = fetchTarball {
    url = "https://download-cdn.jetbrains.com/language-server/kotlin-server/${finalAttrs.version}/kotlin-server-${finalAttrs.version}.tar.gz";
    sha256 = "0w4jflkpy39s08lyhf3gpcq3k5jfhjbpxx7xc1q76sp0pf0633il";
  };

  nativeBuildInputs = [
    makeWrapper
    autoPatchelfHook
  ];

  buildInputs = [
    stdenv.cc.cc.lib
  ];

  installPhase = ''
    runHook preInstall

    mkdir -p $out/{bin,share}
    cp -r bin lib license modules plugins product-info.json $out/share
    ln -sT "${jdk}"/lib/openjdk $out/share/jbr

    makeWrapper $out/share/bin/intellij-server $out/bin/intellij-server

    runHook postInstall
  '';
})
