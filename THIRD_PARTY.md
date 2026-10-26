# Third-party components and licenses

| Component | Source | License |
|---|---|---|
| QuakeSpasm (Quake engine) | github.com/sezero/quakespasm | GPL-2.0-or-later |
| SDL2 (+ Android Java glue) | github.com/libsdl-org/SDL | zlib |
| gl4es | github.com/ptitSeb/gl4es | MIT |

The native library `libmain.so` links QuakeSpasm (GPL), so the combined work, including the glue code
in this repository, is distributed under GPL-2.0-or-later. Source for QuakeSpasm is fetched by
`scripts/fetch_deps.sh`; the exact commits used are written to `third_party/deps-resolved.txt`.

Quake game data (pak0.pak etc.) is copyrighted by id Software / ZeniMax and is NOT included.
You must supply your own copy (shareware or retail).
