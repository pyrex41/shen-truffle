# shen-truffle

`shen-truffle` is a Shen 41.2 implementation built on the GraalVM Truffle
language framework. It is an embeddable Polyglot language and a small command
line program, suitable for use directly or as a Bifrost/Yggdrasil backend.

The runtime targets GraalVM 25 (Java 25) and keeps Shen's KLambda evaluator,
closures, currying, tail calls, Prolog, and standard library semantics. The
Shen 41.2 kernel and standard-library sources are packaged with the assembled
distribution, so an installed launcher does not depend on a checkout.

## Quick start

Install [GraalVM for JDK 25](https://www.graalvm.org/jdk25/) and
[Maven](https://maven.apache.org/), then:

```sh
mvn -B package
scripts/shen-truffle --version
scripts/shen-truffle                 # interactive REPL
scripts/shen-truffle eval -e '(+ 2 3)'
scripts/shen-truffle script path/to/program.shen
```

`JAVA_HOME` (or `JAVACMD`) selects the JVM. A Windows equivalent is
`scripts\shen-truffle.cmd`. The launcher prefers `target/shen-truffle/` from
the Maven assembly and falls back to `target/classes` for development.

The standard library is loaded by default. Embedders can select kernel-only
operation with `ShenRuntime.builder().standardLibrary(false)`.

## CLI

The stable command surface is:

```text
shen-truffle [repl]
shen-truffle eval [-q] [-e EXPR] [-l FILE]
shen-truffle script FILE [ARGS...]
shen-truffle --help | --version
```

Top-level `-e` and `-l` remain accepted for compatibility with older scripts.
`--version` reports the Shen kernel, port, Java, and GraalVM versions.

## Embedding

Java applications can use `com.github.ragnard.shen.ShenRuntime`:

```java
try (var shen = ShenRuntime.builder().build()) {
    var value = shen.eval("(+ 2 3)");
    System.out.println(value.asInt());
}
```

The API returns `org.graalvm.polyglot.Value`, exposes executable functions and
proper lists through Polyglot interop, and keeps bindings context-local.
Contexts are single-thread confined; create one runtime per concurrent thread.

## Building and testing

```sh
mvn -B verify                         # compile, unit tests, package checks
mvn -B -Pshen verify                  # run the vendored Shen 41.2 kernel suite
mvn -B -Pnative package               # optional Native Image distribution
scripts/shen-truffle --version
```

The `shen` Maven profile runs the vendored canonical Shen 41.2 corpus and
requires a zero-failure, 100% report. Bifrost exercises the CLI, arithmetic,
recursion, errors, quiet mode, and file behavior. Yggdrasil exposes `truffle`
(a relocatable JVM app directory) and `truffle-native` (a standalone Native
Image executable) targets.

## Project layout

* `src/main/java` — Truffle AST, parser, runtime, and Polyglot entry point.
* `src/main/resources/klambda` — boot kernel resources.
* `src/main/resources/stlib` — bundled Shen standard-library sources.
* `src/test/resources/kernel-tests` — vendored Shen 41.2 certification corpus.
* `scripts/` — portable POSIX and Windows launchers.

## License and acknowledgements

The implementation is released under the BSD 3-Clause License; see
[`LICENSE`](LICENSE). Shen is the work of [Dr Mark Tarver](http://marktarver.com).
Kernel and standard-library files retain their upstream notices and licenses;
the assembled distribution includes those notices and provenance metadata.

References: [Shen](https://www.shenlanguage.org/), [GraalVM Truffle](https://www.graalvm.org/jdk25/graalvm-as-a-platform/language-implementation-framework/),
[shen-go](https://github.com/pyrex41/shen-go), and
[shen-lua](https://github.com/pyrex41/shen-lua).
## Optional Nix environment

Nix is optional; the normal shen-truffle build and launcher commands continue to work
with tools installed by any method. For a pinned development toolchain:

```sh
nix develop
```

The flake also exports `packages.toolchain` for composition by
[Bifrost](https://github.com/pyrex41/bifrost):

```sh
nix shell .#toolchain
```

If direnv is installed, `direnv allow` opts this checkout into the same dev
shell automatically. Nothing activates until that explicit authorization, and
Nix is never required at runtime.
