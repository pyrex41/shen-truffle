package com.github.ragnard.shen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

/** Command-line entry point for Shen (ShenOSKernel 43.1). */
public final class Shen {
    private final ShenRuntime embedded;
    public Shen() { embedded = ShenRuntime.builder().build(); }
    public Object eval(String source) { return embedded.eval(source); }

    public static void main(String[] args) {
        if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            usage();
            return;
        }
        if (args.length > 0 && ("--version".equals(args[0]) || "-v".equals(args[0]))) {
            version();
            return;
        }
        try (ShenRuntime runtime = ShenRuntime.builder().build()) {
            int status = run(runtime, args);
            if (status != 0) System.exit(status);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }

    static int run(ShenRuntime rt, String[] argv) throws IOException {
        if (argv.length == 0) return repl(rt);
        if ("--help".equals(argv[0]) || "-h".equals(argv[0])) { usage(); return 0; }
        if ("--version".equals(argv[0]) || "-v".equals(argv[0])) {
            version();
            return 0;
        }
        if ("repl".equals(argv[0])) return repl(rt);
        if ("script".equals(argv[0])) {
            if (argv.length < 2) return error("missing script file");
            rt.configureArgv(java.util.Arrays.copyOfRange(argv, 1, argv.length));
            rt.script(Path.of(argv[1]));
            return 0;
        }

        // `eval` accepts ordered operations. Legacy top-level -e/-l are aliases.
        int i = 0; boolean quiet = false; boolean did = false;
        while (i < argv.length) {
            String a = argv[i++];
            if ("eval".equals(a)) continue;
            if ("-q".equals(a) || "--quiet".equals(a)) { rt.eval("(set *hush* true)"); quiet = true; continue; }
            if ("-e".equals(a) || "--eval".equals(a)) {
                if (i >= argv.length) return error("missing argument to " + a);
                String shown = rt.evalToString(argv[i++]);
                if (!quiet) System.out.println(shown);
                did = true; continue;
            }
            if ("-l".equals(a) || "--load".equals(a)) {
                if (i >= argv.length) return error("missing argument to " + a);
                rt.load(Path.of(argv[i++])); did = true; continue;
            }
            if ("--help".equals(a)) { usage(); return 0; }
            return error("invalid argument: " + a);
        }
        return did ? 0 : repl(rt);
    }

    private static int repl(ShenRuntime rt) throws IOException {
        // A host-side loop gives a deterministic clean EOF for pipes and scripts.
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = r.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            System.out.println(rt.evalToString(line));
        }
        return 0;
    }

    private static int error(String s) { System.err.println("ERROR: " + s); return 1; }
    private static void version() {
        System.out.println("Shen 43.1 (shen-truffle, GraalVM " + System.getProperty("java.vm.version") + ")");
    }
    private static void usage() {
        System.out.println("Usage: shen-truffle [--help|--version] [repl|script FILE [ARGS...]|eval OPTIONS]");
        System.out.println("  -e, --eval EXPR   evaluate expression (repeatable, ordered)");
        System.out.println("  -l, --load FILE   load Shen source file (repeatable, ordered)");
        System.out.println("  -q, --quiet       suppress eval output");
    }
}
