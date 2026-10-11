# Standard-library provenance

## Current: ShenOSKernel 42.2

The sources in this directory are vendored byte-for-byte from the
ShenOSKernel 42.2 release (`Shen-Language/shen-sources`, tag `shen-42.2`,
commit `73507e069fe147aefb728d632c4ce40bc6a7b314`; `ShenOSKernel-42.2.tar.gz`
SHA-256 `1d02273003654d34ec020ae67cb214ecec410dee3350419ff807d82b652678d8`):
`lib/stlib/` plus `lib/LICENSE` and `lib/README.txt`.

They are distributed for reference. At runtime the port boots the
precompiled `klambda/stlib.kl` from the same release and calls
`stlib.initialise` (see `../klambda/PROVENANCE.md`).

## History

The previous Shen 42.0 StLib sources came byte-for-byte from
`pyrex41/shen-upstream`, tag `s42-pristine-20260825`, commit
`28825a211f5b2fb952510dab17267ca8eb9594a0` (archive SHA-256
`30abdc7e5a1e27b7a20109c1ed141e4712885e31f24d9710d16415fbbd4dfb23`), and were
compiled from source at every boot.
