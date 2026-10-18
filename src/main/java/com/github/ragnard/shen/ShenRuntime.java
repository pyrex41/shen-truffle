package com.github.ragnard.shen;

import com.github.ragnard.shen.klambda.Language;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.*;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/** Embeddable Shen runtime. A runtime owns one Polyglot context and is not thread safe. */
public final class ShenRuntime implements AutoCloseable {
    private final Context context;
    private final Path workingDirectory;

    private ShenRuntime(Builder b) {
        this.workingDirectory = b.workingDirectory.toAbsolutePath().normalize();
        Context.Builder cb = Context.newBuilder(Language.ID).allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .in(b.in).out(b.out).err(b.err);
        this.context = cb.build();
        if (b.bootKernel) {
            loadKernel();
            if (b.standardLibrary) loadBundledLibrary();
            // stlib.initialise ends with (cd ""), so the home directory is set last.
            configureHomeDirectory();
        } else if (b.standardLibrary) {
            throw new IllegalArgumentException("standardLibrary requires bootKernel");
        }
    }

    /**
     * ShenOSKernel 43.1 modules. They contain only defuns, so their order is
     * irrelevant; {@code shen.initialise} (init.kl) builds the environment,
     * the dict-backed property vector, arities and lambda forms afterwards.
     */
    static final String[] KERNEL_MODULES = {"sys.kl", "writer.kl", "core.kl", "reader.kl", "declarations.kl",
            "toplevel.kl", "macros.kl", "load.kl", "prolog.kl", "sequent.kl", "track.kl", "t-star.kl", "yacc.kl",
            "types.kl", "dict.kl", "init.kl", "extension-launcher.kl"};

    private void loadKernel() {
        for (String n : KERNEL_MODULES) evalResource("klambda/" + n);
        installPortOverrides();
        evalRaw("(shen.initialise)", "<shen.initialise>");
    }

    private void evalResource(String name) {
        try (InputStream in = ShenRuntime.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException("missing kernel resource " + name);
            evalRaw(new String(in.readAllBytes(), StandardCharsets.UTF_8), name);
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    /** Port-specific behavior which cannot be expressed by the portable kernel primitives. */
    void installPortOverrides() {
        // The portable pr/2 gates every stream on *hush*.  A launcher must
        // hush only its standard output so -q never discards explicit file IO.
        evalRaw("(defun pr (X Stream) (if (and (value *hush*) (= Stream (value *stoutput*))) X " +
                "(if (char-stream? Stream) (write-string X Stream) " +
                "(shen.write-chars X Stream (shen.string->byte X 0) 1))))", "<port-overrides>");
        // The kernel's symbol? parses (str X); only host symbols may qualify,
        // otherwise an anonymous function (printed <function>) is a symbol.
        evalRaw("(defun symbol? (X) (if (shen.truffle.symbol-object? X) (if (boolean? X) false " +
                "(if (element? X (cons { (cons } (cons (intern \":\") (cons (intern \";\") (cons (intern \",\") ())))))) true " +
                "(trap-error (shen.analyse-symbol? (str X)) (lambda E false)))) false))", "<port-overrides>");
        // The kernel hashes (str X); hash equal numbers (1 and 1.0) alike so
        // hash agrees with = and dict lookups (get/put) find them.
        evalRaw("(defun hash (X Bound) (let H (shen.mod (shen.hashkey (shen.truffle.normalise-number X)) Bound) " +
                "(if (= H 0) 1 H)))", "<port-overrides>");
    }

    /**
     * A KLambda expression evaluating to {@code s}. KL string literals have no
     * escapes, so a double quote is spliced in with {@code (n->string 34)}.
     */
    static String klString(String s) {
        String[] parts = s.split("\"", -1);
        String expr = "\"" + parts[parts.length - 1] + "\"";
        for (int i = parts.length - 2; i >= 0; i--)
            expr = "(cn \"" + parts[i] + "\" (cn (n->string 34) " + expr + "))";
        return expr;
    }
    void configureHomeDirectory() {
        evalRaw("(set *home-directory* " + klString(workingDirectory.toString()) + ")", "<configure-home>");
    }
    void configureArgv(String[] args) {
        StringBuilder b = new StringBuilder("(set *argv* ");
        for (int i = 0; args != null && i < args.length; i++) b.append("(cons ").append(klString(args[i])).append(" ");
        b.append("()").append(")".repeat(Math.max(0, args == null ? 0 : args.length) + 1));
        evalRaw(b.toString(), "<configure-argv>");
    }
    private void loadBundledLibrary() {
        // ShenOSKernel 43.1 ships the standard library precompiled as
        // klambda/stlib.kl; stlib.initialise installs its packages, arities,
        // lambda forms, macros, datatypes and types (doc/port-upgrades.md 41.1).
        try {
            evalResource("klambda/stlib.kl");
            evalRaw("(stlib.initialise)", "<stlib.initialise>");
        } catch (RuntimeException e) {
            throw new IllegalStateException("failed to load bundled standard library", e);
        }
    }

    public static Builder builder() { return new Builder(); }
    /** Evaluate one Shen expression in the booted runtime. */
    public Value eval(String source) {
        return evalRaw("(shen.x.launcher.eval-string " + klString(source) + ")", "<eval>");
    }
    /** Evaluate one Shen expression and render the result with Shen's own printer (as {@code ~A}). */
    public String evalToString(String source) {
        return evalRaw("(shen.app (shen.x.launcher.eval-string " + klString(source) + ") \"\" shen.a)", "<eval>").asString();
    }
    /** Internal loader path for KL resources and shaken slices. */
    Value evalRaw(String source, String name) {
        try { return context.eval(Source.newBuilder(Language.ID, source, name).build()); }
        catch (IOException e) { throw new IllegalArgumentException("invalid Shen source", e); }
    }
    public Value load(Path source) {
        Path resolved = source.isAbsolute() ? source : workingDirectory.resolve(source);
        return evalRaw("(load " + klString(resolved.normalize().toString()) + ")", source.toString());
    }
    /**
     * Runs a script as the kernel launcher's {@code script} command does:
     * each toplevel form is evaluated without echoing its value.
     */
    public Value script(Path source) {
        Path resolved = source.isAbsolute() ? source : workingDirectory.resolve(source);
        return evalRaw("(shen.x.launcher.quiet-load " + klString(resolved.normalize().toString()) + ")", source.toString());
    }
    public Path workingDirectory() { return workingDirectory; }
    Context context() { return context; }
    @Override public void close() {
        context.close();
    }

    public static final class Builder {
        private InputStream in = System.in; private OutputStream out = System.out, err = System.err;
        private Path workingDirectory = Path.of(System.getProperty("user.dir")); private boolean standardLibrary = true; private boolean bootKernel = true;
        public Builder in(InputStream v) { in = v == null ? System.in : v; return this; }
        public Builder out(OutputStream v) { out = v == null ? System.out : v; return this; }
        public Builder err(OutputStream v) { err = v == null ? System.err : v; return this; }
        public Builder workingDirectory(Path v) { if (v != null) workingDirectory = v; return this; }
        public Builder standardLibrary(boolean v) { standardLibrary = v; return this; }
        /** Internal hook for shaken images that provide their own kernel. */
        public Builder bootKernel(boolean v) { bootKernel = v; return this; }
        public ShenRuntime build() { return new ShenRuntime(this); }
    }
}
