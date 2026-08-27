package com.github.ragnard.shen;

import org.graalvm.polyglot.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Command-line entry point for Shen 42.0. */
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
            setArgv(rt, argv, 1);
            rt.eval("(load \"" + quote(argv[1]) + "\")");
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
                Value v = rt.eval(argv[i++]);
                if (!quiet) print(v);
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
            print(rt.eval(line));
        }
        return 0;
    }

    private static void setArgv(ShenRuntime rt, String[] args, int fileIndex) {
        List<String> values = new ArrayList<>();
        for (int i = fileIndex; i < args.length; i++) values.add(args[i]);
        StringBuilder expr = new StringBuilder("(set *argv* ");
        for (int i = 0; i < values.size(); i++) expr.append("(cons \"").append(quote(values.get(i))).append("\" ");
        expr.append("()");
        for (int i = 0; i < values.size(); i++) expr.append(')');
        expr.append(')');
        rt.eval(expr.toString());
    }

    private static void print(Value v) { if (v != null && !v.isNull()) System.out.println(v); }
    private static String quote(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static int error(String s) { System.err.println("ERROR: " + s); return 1; }
    private static void version() {
        System.out.println("Shen 42.0 (shen-truffle, GraalVM " + System.getProperty("java.vm.version") + ")");
    }
    private static void usage() {
        System.out.println("Usage: shen-truffle [--help|--version] [repl|script FILE [ARGS...]|eval OPTIONS]");
        System.out.println("  -e, --eval EXPR   evaluate expression (repeatable, ordered)");
        System.out.println("  -l, --load FILE   load Shen source file (repeatable, ordered)");
        System.out.println("  -q, --quiet       suppress eval output");
    }
}
