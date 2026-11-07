# Provenance

## Current: ShenOSKernel 43.4

These KLambda files are vendored byte-for-byte from the community
`ShenOSKernel-43.4` release at `Shen-Language/shen-sources`, tag `shen-43.4`
(tag object `40fd335893cf`), commit `f34b307b490880eedcfe1f5e5d3cc24ca3d2602e`. The verified release archive
SHA-256 is `15e7972e6bf0cd65d37c2eba85750b8839d2dbadda41027eae0c88e5017a2756`.

The port loads the 43.4 runtime kernel modules, then calls `shen.initialise`;
the standard library is the release's precompiled
`stlib.kl`. The 43.4 public character stream functions are implemented by the
host (`char-stream?`, `read-unit-string`, and `write-string`).
