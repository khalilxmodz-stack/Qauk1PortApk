#!/usr/bin/env bash
# Fetches pinned third-party sources into third_party/ and copies SDL's Java glue.
set -euo pipefail
cd "$(dirname "$0")/.."
rm -rf third_party && mkdir -p third_party
: > third_party/deps-resolved.txt

while read -r name url ref mode; do
  case "$name" in ''|\#*) continue;; esac
  dest="third_party/$name"
  echo "== $name $ref ($mode)"
  git init -q "$dest"
  git -C "$dest" remote add origin "$url"
  if git -C "$dest" fetch -q --depth 1 origin "$ref"; then
    git -C "$dest" checkout -q FETCH_HEAD
  elif [ "$mode" = "loose" ]; then
    echo "WARNING: could not fetch '$ref' for $name; using default branch (NOT reproducible)"
    git -C "$dest" fetch -q --depth 1 origin HEAD
    git -C "$dest" checkout -q FETCH_HEAD
  else
    echo "ERROR: could not fetch '$ref' from $url"; exit 1
  fi
  echo "$name $url $ref $(git -C "$dest" rev-parse HEAD)" | tee -a third_party/deps-resolved.txt
done < deps.lock

# SDL Java glue must match the SDL native version, so copy it from the same checkout.
SDLJ=third_party/SDL2/android-project/app/src/main/java/org/libsdl/app
test -f "$SDLJ/SDLActivity.java" || { echo "ERROR: SDLActivity.java not found in SDL checkout"; exit 1; }
mkdir -p app/src/main/java/org/libsdl/app
cp "$SDLJ"/*.java app/src/main/java/org/libsdl/app/

# Sanity checks for the QuakeSpasm layout this project's CMake expects.
test -f third_party/quakespasm/Quake/quakedef.h || { echo "ERROR: Quake/quakedef.h missing"; exit 1; }
test -f third_party/quakespasm/Quake/in_sdl.c   || { echo "ERROR: Quake/in_sdl.c missing";   exit 1; }
echo "main() definitions in QuakeSpasm:"; grep -ln '^int main' third_party/quakespasm/Quake/*.c || true
echo "fetch_deps OK"
