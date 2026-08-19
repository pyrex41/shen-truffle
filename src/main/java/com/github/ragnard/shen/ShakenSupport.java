package com.github.ragnard.shen;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Runtime hooks used by Yggdrasil's shaken launcher. */
public final class ShakenSupport {
    private ShakenSupport() {}

    public static void run(Path dir, String[] args) throws IOException {
        Manifest m = Manifest.read(dir);
        try (ShenRuntime rt = ShenRuntime.builder().workingDirectory(dir).standardLibrary(false).bootKernel(false).build()) {
            rt.evalRaw(Files.readString(resolveSafe(dir, m.kernel)), m.kernel);
            rt.installPortOverrides();
            rt.configureHomeDirectory();
            if (m.init != null && !m.init.isEmpty()) rt.evalRaw("(" + m.init + ")", "<shaken-init>");
            rt.configureArgv(args);
            for (String f : m.users) rt.evalRaw(Files.readString(resolveSafe(dir, f)), f);
        }
    }

    /** Loads shaken resources from the class path (typically Native Image resources). */
    public static void runResources(String root, String[] args) throws IOException {
        ClassLoader cl = ShakenSupport.class.getClassLoader();
        String manifestName = root + "/yggdrasil.manifest.txt";
        Manifest m;
        try (InputStream mi = cl.getResourceAsStream(manifestName)) {
            if (mi == null) throw new FileNotFoundException(manifestName);
            m = new Manifest(new String(mi.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (ShenRuntime rt = ShenRuntime.builder().standardLibrary(false).bootKernel(false).build()) {
            try (InputStream kernel = cl.getResourceAsStream(root + "/" + m.kernel)) {
                if (kernel == null) throw new FileNotFoundException(root + "/" + m.kernel);
                rt.evalRaw(new String(kernel.readAllBytes(), StandardCharsets.UTF_8), m.kernel);
            }
            rt.installPortOverrides();
            rt.configureHomeDirectory();
            if (m.init != null && !m.init.isEmpty()) rt.evalRaw("(" + m.init + ")", "<shaken-init>");
            rt.configureArgv(args);
            for (String f : m.users) {
                try (InputStream in = cl.getResourceAsStream(root + "/" + f)) {
                    if (in == null) throw new FileNotFoundException(root + "/" + f);
                    rt.evalRaw(new String(in.readAllBytes(), StandardCharsets.UTF_8), f);
                }
            }
        }
    }

    private static Path resolveSafe(Path dir, String name) throws IOException {
        Path base = dir.toAbsolutePath().normalize();
        Path p = base.resolve(name == null ? "" : name).normalize();
        if (name == null || name.isEmpty() || !p.startsWith(base) || p.equals(base))
            throw new IOException("invalid manifest path: " + name);
        return p;
    }

    private static final class Manifest {
        final String kernel; final String init; final List<String> users;
        Manifest(String text) throws IOException {
            String k = "kernel.kl", i = null; List<String> u = new ArrayList<>();
            for (String raw : text.split("\\R")) {
                String line = raw.trim(); if (line.isEmpty() || line.startsWith("#")) continue;
                int p = line.indexOf('='); if (p <= 0) throw new IOException("malformed manifest line: " + raw);
                String key = line.substring(0,p).trim(), val = line.substring(p+1).trim();
                switch (key) { case "kernel": k = val; break; case "init": i = val; break; case "user": u.add(val); break; default: }
            }
            if (k.contains("..") || k.startsWith("/") || u.stream().anyMatch(x -> x.contains("..") || x.startsWith("/"))) throw new IOException("unsafe manifest path");
            kernel = k; init = i; users = List.copyOf(u);
        }
        List<String> files() { List<String> x = new ArrayList<>(); x.add(kernel); x.addAll(users); return x; }
        static Manifest read(Path dir) throws IOException { return new Manifest(Files.readString(dir.resolve("yggdrasil.manifest.txt"))); }
    }
}
