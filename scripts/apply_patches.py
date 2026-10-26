#!/usr/bin/env python3
"""Patches QuakeSpasm's in_sdl.c so the Android touch layer feeds the real input path.
Fails loudly if an anchor is not found (never silently skips)."""
import re, sys, pathlib

path = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "third_party/quakespasm/Quake/in_sdl.c")
src = path.read_text(encoding="utf-8", errors="replace")

if "IN_AndroidMove" in src:
    print("already patched"); sys.exit(0)

def patch(src, sig_regex, call, proto):
    m = re.search(sig_regex, src, re.M)
    if not m:
        print("ERROR: anchor not found: " + sig_regex)
        for i, l in enumerate(src.splitlines(), 1):
            if "IN_Move" in l or "IN_Commands" in l:
                print("  line %d: %s" % (i, l))
        sys.exit(1)
    brace = src.index("{", m.end())
    src = src[:brace + 1] + "\n\t" + call + src[brace + 1:]
    return src[:m.start()] + proto + "\n" + src[m.start():]

src = patch(src, r"^void\s+IN_Move\s*\(\s*usercmd_t\s*\*\s*cmd\s*\)",
            "IN_AndroidMove(cmd);", "extern void IN_AndroidMove(usercmd_t *cmd);")
src = patch(src, r"^void\s+IN_Commands\s*\(\s*void\s*\)",
            "IN_AndroidCommands();", "extern void IN_AndroidCommands(void);")
path.write_text(src, encoding="utf-8")
print("patched " + str(path))
