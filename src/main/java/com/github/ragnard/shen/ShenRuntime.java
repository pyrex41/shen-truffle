package com.github.ragnard.shen;

import com.github.ragnard.shen.klambda.Language;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/** Embeddable Shen runtime. A runtime owns one Polyglot context and is not thread safe. */
public final class ShenRuntime implements AutoCloseable {
    private final Context context;
    private final Path workingDirectory;
    private Path libraryTemp;

    private ShenRuntime(Builder b) {
        this.workingDirectory = b.workingDirectory.toAbsolutePath().normalize();
        Context.Builder cb = Context.newBuilder(Language.ID).allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .in(b.in).out(b.out).err(b.err);
        this.context = cb.build();
        if (b.bootKernel) {
            loadKernel();
            configureHomeDirectory();
            if (b.standardLibrary) loadBundledLibrary();
        } else if (b.standardLibrary) {
            throw new IllegalArgumentException("standardLibrary requires bootKernel");
        }
    }

    private void loadKernel() {
        String[] names = {"sys.kl", "writer.kl", "core.kl", "reader.kl", "declarations.kl", "toplevel.kl", "macros.kl", "load.kl", "prolog.kl", "sequent.kl", "track.kl", "t-star.kl", "yacc.kl", "types.kl", "extension-launcher.kl"};
        for (String n : names) try (InputStream in = ShenRuntime.class.getClassLoader().getResourceAsStream("klambda/" + n)) {
            if (in == null) throw new IllegalStateException("missing kernel resource " + n);
            evalRaw(new String(in.readAllBytes(), StandardCharsets.UTF_8), n);
        } catch (IOException e) { throw new UncheckedIOException(e); }
        installPortOverrides();
    }

    /** Port-specific behavior which cannot be expressed by the portable kernel primitives. */
    void installPortOverrides() {
        // Tarver's portable pr/2 gates every stream on *hush*.  A launcher must
        // hush only its standard output so -q never discards explicit file IO.
        evalRaw("(defun pr (X Stream) (if (and (value *hush*) (= Stream (value *stoutput*))) X " +
                "(if (shen.char-stoutput? Stream) (shen.write-string X Stream) " +
                "(shen.write-chars X Stream (shen.string->byte X 0) 1))))", "<port-overrides>");
    }

    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
    void configureHomeDirectory() {
        evalRaw("(set *home-directory* \"" + escape(workingDirectory.toString()) + "\")", "<configure-home>");
    }
    void configureArgv(String[] args) {
        StringBuilder b = new StringBuilder("(set *argv* ");
        for (int i = 0; args != null && i < args.length; i++) b.append("(cons \"").append(escape(args[i])).append("\" ");
        b.append("()").append(")".repeat(Math.max(0, args == null ? 0 : args.length) + 1));
        evalRaw(b.toString(), "<configure-argv>");
    }
    private void loadBundledLibrary() {
        // Match Tarver's S42 Lib/StLib/install.shen order, including datatype
        // declarations and the IO/package modules required by later sources.
        String[] names = {"stlib/Symbols/symbols1.shen", "stlib/Symbols/symbols2.shen",
                "stlib/Maths/macros.shen", "stlib/Maths/maths.shen",
                "stlib/Maths/rationals.dtype", "stlib/Maths/rationals.shen",
                "stlib/Maths/complex.dtype", "stlib/Maths/complex.shen",
                "stlib/Maths/numerals.dtype", "stlib/Maths/numerals.shen",
                "stlib/Lists/lists.shen", "stlib/Strings/macros.shen",
                "stlib/Strings/strings.shen", "stlib/Strings/smart.shen",
                "stlib/Vectors/macros.shen", "stlib/IO/prettyprint.shen",
                "stlib/IO/delete-file.shen", "stlib/IO/files.shen",
                "stlib/Tuples/tuples.shen", "stlib/package-stlib.shen"};
        try { libraryTemp = Files.createTempDirectory("shen-stlib-"); } catch (IOException e) { throw new UncheckedIOException(e); }
        for (String n : names) try (InputStream in = ShenRuntime.class.getClassLoader().getResourceAsStream(n)) {
            if (in == null) throw new IllegalStateException("missing standard library resource " + n);
            Path target = libraryTemp.resolve(n.substring("stlib/".length()));
            Files.createDirectories(target.getParent());
            Files.write(target, in.readAllBytes());
            // Shen source must go through Shen's reader/compiler, not the raw KL parser.
            // The launcher helper avoids the normal per-definition load chatter.
            try {
                evalRaw("(shen.x.launcher.quiet-load \"" + escape(target.toString()) + "\")", n);
            } catch (RuntimeException e) {
                throw new IllegalStateException("failed to load bundled standard library resource " + n, e);
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static Builder builder() { return new Builder(); }
    /** Evaluate one Shen expression in the booted runtime. */
    public Value eval(String source) {
        return evalRaw("(shen.x.launcher.eval-string \"" + escape(source) + "\")", "<eval>");
    }
    /** Internal loader path for KL resources and shaken slices. */
    Value evalRaw(String source, String name) {
        try { return context.eval(Source.newBuilder(Language.ID, source, name).build()); }
        catch (IOException e) { throw new IllegalArgumentException("invalid Shen source", e); }
    }
    public Value load(Path source) {
        Path resolved = source.isAbsolute() ? source : workingDirectory.resolve(source);
        return evalRaw("(load \"" + escape(resolved.normalize().toString()) + "\")", source.toString());
    }
    public Path workingDirectory() { return workingDirectory; }
    Context context() { return context; }
    @Override public void close() {
        context.close();
        if (libraryTemp != null) try (var walk = Files.walk(libraryTemp)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) { }
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
