# Provenance

## Current: ShenOSKernel 43.2

These KLambda files are vendored byte-for-byte from the community
`ShenOSKernel-43.2` release at `Shen-Language/shen-sources`, tag `shen-43.2`
(tag object `7b7563e5`), commit `e6947f507ebc`. The verified release archive
SHA-256 is `23c71508f0de24785e1f127ea4fab440081904d81c463090bc642ee840417f32`.

The port loads the 43.2 runtime kernel modules, then calls `shen.initialise`;
the standard library is the release's precompiled
`stlib.kl`. The 43.2 public character stream functions are implemented by the
host (`char-stream?`, `read-unit-string`, and `write-string`).
