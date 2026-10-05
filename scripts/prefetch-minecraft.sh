#!/usr/bin/env bash
set -euo pipefail

# Loom 1.18.2's concurrent client/server download progress logger can fail on
# Gradle 9.8. Fetch and verify the official jars before Loom starts that work.
target="${1:?Usage: prefetch-minecraft.sh <minecraft>-fabric}"
if [[ ! "$target" =~ ^([0-9]+\.[0-9]+(\.[0-9]+)?)-fabric$ ]]; then
  echo "Expected a registered Fabric release target, got: $target" >&2
  exit 1
fi
minecraft="${BASH_REMATCH[1]}"
cache="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/fabric-loom/$minecraft"
scratch="$(mktemp -d)"
cleanup() {
  rm -f -- "$scratch/manifest.json" "$scratch/version.json" "$scratch/client.jar" "$scratch/server.jar"
  rmdir -- "$scratch"
}
trap cleanup EXIT
fetch() {
  curl --fail --location --silent --show-error --retry 5 --retry-all-errors \
    --connect-timeout 20 --max-time 300 "$1" --output "$2"
}

fetch 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json' "$scratch/manifest.json"
version_url="$(jq -er --arg version "$minecraft" '.versions[] | select(.id == $version) | .url' "$scratch/manifest.json")"
fetch "$version_url" "$scratch/version.json"
mkdir -p -- "$cache"

for kind in client server; do
  url="$(jq -er --arg kind "$kind" '.downloads[$kind].url' "$scratch/version.json")"
  hash="$(jq -er --arg kind "$kind" '.downloads[$kind].sha1' "$scratch/version.json")"
  destination="$cache/minecraft-$kind.jar"
  if [[ -f "$destination" ]] && printf '%s  %s\n' "$hash" "$destination" | sha1sum --check --status; then
    echo "Verified cached Minecraft $minecraft $kind"
    continue
  fi
  fetch "$url" "$scratch/$kind.jar"
  printf '%s  %s\n' "$hash" "$scratch/$kind.jar" | sha1sum --check --status
  mv -f -- "$scratch/$kind.jar" "$destination"
  echo "Verified Minecraft $minecraft $kind"
done
