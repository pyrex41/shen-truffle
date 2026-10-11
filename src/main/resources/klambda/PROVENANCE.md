# Provenance

## Current: ShenOSKernel 42.2 (community `shen-sources` line)

The KLambda files in this directory are vendored **byte-for-byte** from the
ShenOSKernel 42.2 release, the kernel shipped by upstream shen-scheme 0.50:

- Source: [`Shen-Language/shen-sources`](https://github.com/Shen-Language/shen-sources),
  tag `shen-42.2`, commit `73507e069fe147aefb728d632c4ce40bc6a7b314`
- Release asset: `ShenOSKernel-42.2.tar.gz`, SHA-256
  `1d02273003654d34ec020ae67cb214ecec410dee3350419ff807d82b652678d8`
  (the checksum pinned in shen-scheme 0.50's Makefile)
- Files: every `klambda/*.kl` from the release (16 kernel modules including
  `dict.kl` and `init.kl`, the precompiled standard library `stlib.kl`, and
  the `extension-*.kl` files). No file is edited.
- `(version)` is `"42.2"`, set by `shen.initialise` in `init.kl`.

### Lineage choice

pyrex41/bifrost#26 offered two options: (a) adopt ShenOSKernel 42.2
wholesale, or (b) port the 42.1/42.2 changes onto Tarver's S42.0. This port
takes **option (a)**. The community kernel is structurally different from
Tarver's S42.0: its modules contain only `defun`s, initialisation lives in
`shen.initialise` (`init.kl`), and the property vector is a `shen.dict`
(`dict.kl`) rather than Tarver's vector store.

### Boot sequence (`ShenRuntime`)

1. Load the 16 kernel modules plus `extension-launcher.kl` (order no longer
   matters).
2. Install port overrides (`pr`, so `*hush*` gates only standard output;
   `symbol?`, so only host symbols qualify; `hash`, so `=` numbers such as
   `1` and `1.0` hash alike). Each keeps the kernel definition's result
   otherwise.
3. `(shen.initialise)`.
4. With the standard library: load `stlib.kl`, then `(stlib.initialise)`.
5. Set `*home-directory*` (`stlib.initialise` ends with `(cd "")`).

### 42.1/42.2 port audit

- No native `macroexpand`, `shen.macroexpand-h`, `shen.try-parse`, reader or
  `read`/`lineread` overrides exist, so the 42.1 source-form handler order is
  the kernel's own. `read-byte` returns -1 at end of stream, so a final form
  without a trailing newline is read.
- No native `get`/`put`/`unput`/`fn`/`arity`/lambda-form store, and no
  `shen.*lambdatable*` or `shen.set-lambda-form-entry` shim: the kernel's
  dict-backed implementations are used unchanged, so the 42.0
  `update-lambda-table` invalidation rule holds (an arity 1 to 0 redefinition
  makes `(fn F)` call the new definition).
- The namespaces extension is vendored but not booted.
- The host KLambda reader treats string literals as raw text (KLambda has no
  escapes); `stlib.kl` contains the one-character string `"\"`.

## History: Tarver S42.0 (until this upgrade)

Canonical Shen 42.0 KLambda modules were vendored byte-for-byte from the
`pyrex41/shen-upstream` mirror, tag `s42-pristine-20260825`, commit
`28825a211f5b2fb952510dab17267ca8eb9594a0` (archive SHA-256
`30abdc7e5a1e27b7a20109c1ed141e4712885e31f24d9710d16415fbbd4dfb23`).
The 15 upstream modules (including `backend.kl`) were loaded in the order
required by Tarver's S42 distribution, which keeps initialisation inline.
