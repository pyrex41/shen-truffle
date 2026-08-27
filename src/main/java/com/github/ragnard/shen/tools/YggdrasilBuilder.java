package com.github.ragnard.shen.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Stage-2 packager for a Yggdrasil shaken directory.
 *
 * The builder deliberately contains no Shen implementation details.  It
 * validates the stage-1 manifest, preserves the listed load order, and emits
 * a relocatable application whose launcher delegates to ShakenLauncher.
 */
public final class YggdrasilBuilder {
    /** Tarver Shen 42.0 distribution emitted by Yggdrasil stage 1. */
    private static final String KERNEL_VERSION = "42-s42.20260825";
    private YggdrasilBuilder() {}

    public static void main(String[] args) throws Exception {
        Options o = Options.parse(args);
        Manifest m = Manifest.read(o.shaken);
        if ("jvm".equals(o.format)) packageJvm(o, m);
        else packageNative(o, m);
    }

    private static void packageJvm(Options o, Manifest m) throws IOException {
        Files.createDirectories(o.output);
        Path app = o.output;
        Path shaken = app.resolve("shaken");
        Files.createDirectories(shaken);
        Files.writeString(shaken.resolve("yggdrasil.manifest.txt"), m.source, StandardCharsets.UTF_8);
        for (String f : m.files()) {
            Path src = o.shaken.resolve(f).normalize();
            Path dst = shaken.resolve(f).normalize();
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
        Path classes = app.resolve("classes");
        if (o.runtime != null) {
            if (Files.isDirectory(o.runtime)) {
                copyRuntime(o.runtime, classes);
            } else {
                Path lib = app.resolve("lib");
                Files.createDirectories(lib);
                Files.copy(o.runtime, lib.resolve("shen-truffle.jar"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Path bin = app.resolve("bin");
        Files.createDirectories(bin);
        Files.writeString(bin.resolve("shen-truffle"),
                "#!/bin/sh\nset -eu\nAPP=$(CDPATH= cd -- \"$(dirname -- \"$0\")/..\" && pwd)\nJAVA=${JAVACMD:-${JAVA_HOME:+$JAVA_HOME/bin/}java}\nexec \"$JAVA\" -Xss16m --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow ${JAVA_OPTS:-} -cp \"$APP/classes:$APP/lib/*\" com.github.ragnard.shen.tools.ShakenLauncher --dir \"$APP/shaken\" \"$@\"\n",
                StandardCharsets.UTF_8);
        bin.resolve("shen-truffle").toFile().setExecutable(true, false);
        Files.writeString(bin.resolve("shen-truffle.cmd"),
                "@echo off\r\nsetlocal\r\nset APP=%~dp0..\r\nif defined JAVACMD (set JAVA=%JAVACMD%) else if defined JAVA_HOME (set JAVA=%JAVA_HOME%\\bin\\java.exe) else (set JAVA=java)\r\n\"%JAVA%\" -Xss16m --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow %JAVA_OPTS% -cp \"%APP%\\classes;%APP%\\lib\\*\" com.github.ragnard.shen.tools.ShakenLauncher --dir \"%APP%\\shaken\" %*\r\n",
                StandardCharsets.UTF_8);
        Files.writeString(app.resolve("yggdrasil.manifest.txt"), m.source, StandardCharsets.UTF_8);
    }

    private static void packageNative(Options o, Manifest m) throws Exception {
        if (o.runtime == null) throw new IllegalArgumentException("--runtime is required for --format native");
        Path ni = Paths.get(Optional.ofNullable(System.getenv("NATIVE_IMAGE")).orElse("native-image"));
        if (!Files.exists(ni) && !ni.toString().contains("/")) ni = Paths.get("native-image");
        Files.createDirectories(o.output.toAbsolutePath().getParent());
        Path resources = Files.createTempDirectory("shen-truffle-shaken-");
        try {
            Path data = resources.resolve("shaken"); Files.createDirectories(data);
            Files.writeString(data.resolve("yggdrasil.manifest.txt"), m.source, StandardCharsets.UTF_8);
            for (String f : m.files()) { Path d = data.resolve(f); Files.createDirectories(d.getParent()); Files.copy(o.shaken.resolve(f), d); }
            List<String> cmd = new ArrayList<>(); cmd.add(ni.toString());
            cmd.add("-cp"); cmd.add(nativeClasspath(o.runtime, resources));
            cmd.add("--initialize-at-build-time=com.github.ragnard.shen.klambda");
            cmd.add("-H:+UnlockExperimentalVMOptions"); cmd.add("-H:IncludeResources=shaken/.*"); cmd.add("-H:-UnlockExperimentalVMOptions");
            cmd.add("-o"); cmd.add(o.output.toString());
            cmd.add("com.github.ragnard.shen.tools.ShakenLauncher");
            Process p = new ProcessBuilder(cmd).inheritIO().start();
            if (p.waitFor() != 0) throw new IOException("native-image failed with exit code " + p.exitValue());
        } finally { deleteTree(resources); }
    }

    private static String nativeClasspath(Path runtime, Path resources) throws IOException {
        List<Path> entries = new ArrayList<>();
        Path target = Files.isDirectory(runtime) ? runtime.getParent() : runtime.getParent();
        Path classes = target == null ? null : target.resolve("classes");
        Path dependencies = target == null ? null : target.resolve("dependency");
        // A shaded Truffle jar loses module descriptors needed by Native Image.
        // Prefer the original classes plus Maven's copied, modular dependencies.
        if (!Files.isDirectory(runtime) && classes != null && Files.isDirectory(classes)) entries.add(classes);
        else entries.add(runtime);
        if (dependencies != null && Files.isDirectory(dependencies)) {
            try (var jars = Files.list(dependencies)) {
                jars.filter(p -> p.getFileName().toString().endsWith(".jar"))
                        // Native executables use the compact interpreter-only
                        // Truffle runtime; embedding the optimizing compiler
                        // makes arbitrary guest display paths image-build roots.
                        .filter(p -> !p.getFileName().toString().startsWith("truffle-runtime-"))
                        .filter(p -> !p.getFileName().toString().startsWith("truffle-compiler-"))
                        .sorted().forEach(entries::add);
            }
        }
        entries.add(resources);
        return String.join(java.io.File.pathSeparator, entries.stream().map(Path::toString).toList());
    }

    private static void copyRuntime(Path from, Path to) throws IOException {
        if (Files.isDirectory(from)) {
            try (var stream = Files.walk(from)) {
                stream.forEach(p -> { try { Path d = to.resolve(from.relativize(p).toString()); if (Files.isDirectory(p)) Files.createDirectories(d); else { Files.createDirectories(d.getParent()); Files.copy(p,d,StandardCopyOption.REPLACE_EXISTING); } } catch (IOException e) { throw new UncheckedIOException(e); } });
            }
        } else { Files.createDirectories(to); Files.copy(from, to.resolve(from.getFileName()), StandardCopyOption.REPLACE_EXISTING); }
    }
    private static void deleteTree(Path p) throws IOException { if (Files.exists(p)) try (var s=Files.walk(p)) { s.sorted(Comparator.reverseOrder()).forEach(x -> { try { Files.deleteIfExists(x); } catch(IOException e){} }); } }

    static final class Options {
        Path shaken, output, runtime; String format = "jvm";
        static Options parse(String[] a) { Options o=new Options(); List<String> pos=new ArrayList<>(); for(int i=0;i<a.length;i++){ switch(a[i]) { case "--format": o.format=need(a,++i); break; case "--runtime": o.runtime=Paths.get(need(a,++i)); break; case "--help": System.out.println("usage: yggdrasil-build [--format jvm|native] [--runtime PATH] SHAKEN OUT"); System.exit(0); default: pos.add(a[i]); } } if(pos.size()!=2) throw new IllegalArgumentException("expected SHAKEN and OUT"); o.shaken=Paths.get(pos.get(0)).toAbsolutePath(); o.output=Paths.get(pos.get(1)).toAbsolutePath(); if(o.runtime==null){String cp=System.getProperty("java.class.path","");if(!cp.isEmpty())o.runtime=Paths.get(cp.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))[0]).toAbsolutePath();} if(!o.format.equals("jvm")&&!o.format.equals("native")) throw new IllegalArgumentException("format must be jvm or native"); return o; }
        static String need(String[] a,int i){if(i>=a.length)throw new IllegalArgumentException("missing option value");return a[i];}
    }

    static final class Manifest {
        final String source; String kernel="kernel.kl"; String init; final List<String> users=new ArrayList<>();
        private Manifest(String s){source=s;}
        static Manifest read(Path dir) throws IOException { Path p=dir.resolve("yggdrasil.manifest.txt"); if(!Files.isRegularFile(p)) throw new IOException("missing yggdrasil.manifest.txt"); Manifest m=new Manifest(Files.readString(p)); for(String raw:m.source.split("\\R")){String l=raw.trim();if(l.isEmpty()||l.startsWith("#"))continue;int n=l.indexOf('=');if(n<=0)throw new IOException("malformed manifest line: "+raw);String k=l.substring(0,n).trim(),v=l.substring(n+1).trim();switch(k){case "manifest-version": if(!v.equals("3"))throw new IOException("unsupported manifest-version "+v);break;case "kernel-version": if(!v.equals(KERNEL_VERSION))throw new IOException("expected kernel-version "+KERNEL_VERSION+", got "+v);break;case "kernel":m.kernel=v;break;case "init":m.init=v;break;case "user":m.users.add(v);break;default:}}
            List<String> fs=m.files(); Set<String> seen=new HashSet<>(); for(String f:fs){Path q=dir.resolve(f).normalize();if(q.getParent()==null||!q.startsWith(dir)||f.isEmpty()||!seen.add(f)||!Files.isRegularFile(q))throw new IOException("invalid or missing manifest file: "+f);} return m; }
        /** Files are loaded in manifest order; init is a KL function name, not a file. */
        List<String> files(){List<String> x=new ArrayList<>();x.add(kernel);x.addAll(users);return x;}
    }
}
