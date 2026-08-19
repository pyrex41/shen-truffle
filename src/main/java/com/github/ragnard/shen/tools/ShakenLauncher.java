package com.github.ragnard.shen.tools;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Entry point embedded in JVM distributions and used by native-image.
 *
 * Runtime implementations may provide a static
 * {@code com.github.ragnard.shen.ShakenSupport.run(Path,String[])} hook.  The
 * indirection keeps this packager independent of the evolving Truffle APIs;
 * the modern runtime owns parsing, ordered boot, and guest argument wiring.
 */
public final class ShakenLauncher {
    private ShakenLauncher() {}
    public static void main(String[] args) throws Exception {
        Path dir = null; int first = 0;
        if (args.length >= 2 && "--dir".equals(args[0])) { dir = Paths.get(args[1]).toAbsolutePath(); first = 2; }
        if (dir == null) {
            String configured = System.getProperty("shen.shaken.dir", System.getenv("SHEN_SHAKEN_DIR"));
            if (configured != null) dir = Paths.get(configured).toAbsolutePath();
        }
        String[] guest = java.util.Arrays.copyOfRange(args, first, args.length);
        try {
            Class<?> support = Class.forName("com.github.ragnard.shen.ShakenSupport");
            if (dir == null) {
                // Native-image builds embed shaken/* as resources.  The runtime
                // hook is responsible for exposing those resources as a
                // virtual filesystem or temporary directory.
                try {
                    support.getMethod("runResources", String.class, String[].class)
                            .invoke(null, "shaken", guest);
                    return;
                } catch (NoSuchMethodException ignored) {
                    throw new IllegalArgumentException("missing --dir (or shen.shaken.dir)");
                }
            }
            support.getMethod("run", Path.class, String[].class).invoke(null, dir, guest);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("runtime does not provide ShakenSupport.run(Path,String[]); " +
                    "use a modern Shen Truffle runtime", e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause(); if (cause instanceof Exception) throw (Exception) cause; throw e;
        }
    }
}
