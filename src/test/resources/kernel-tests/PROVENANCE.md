# ShenOSKernel 42.2 kernel tests

Vendored byte-for-byte from the `tests/` directory of the ShenOSKernel 42.2
release (`Shen-Language/shen-sources`, tag `shen-42.2`, commit
`73507e069fe147aefb728d632c4ce40bc6a7b314`; `ShenOSKernel-42.2.tar.gz`
SHA-256 `1d02273003654d34ec020ae67cb214ecec410dee3350419ff807d82b652678d8`).
`runme.shen` runs 143 kernel tests; `mvn -Pshen verify` requires all of them
to pass.

Previously: the Shen 42.0 corpus from `pyrex41/shen-upstream` tag
`s42-pristine-20260825`, commit `28825a211f5b2fb952510dab17267ca8eb9594a0`.
