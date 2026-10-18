# Provenance

## Current: ShenOSKernel 43.1

These KLambda files are vendored byte-for-byte from the community
`ShenOSKernel-43.1` release at `Shen-Language/shen-sources`, tag `shen-43.1`,
commit `715824a52b55e9c81a9501df8548ff3edd8411f5`. The verified release archive
SHA-256 is `900eedef44f440af78f9395ed06e689ce1604546fc309f5bc7c8bc84cc651920`.

The port loads the 43.1 runtime kernel modules, then calls `shen.initialise`;
the standard library is the release's precompiled
`stlib.kl`. The 43.1 public character stream functions are implemented by the
host (`char-stream?`, `read-unit-string`, and `write-string`).
