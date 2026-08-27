package com.github.ragnard.shen.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.ragnard.shen.ShakenSupport;
import com.github.ragnard.shen.ShenRuntime;
import org.graalvm.polyglot.Value;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compatibility checks for the public embedding API.  Reflection is
 * intentional: this test remains source-compatible with older checkouts while
 * the runtime is being migrated from PolyglotEngine to Context/Value.
 */
class ModernizationContractTest {
    @Test
    void runtimeExposesBuilderEvalAndLoad() throws Exception {
        Class<?> runtime = ShenRuntime.class;

        Method builder = runtime.getMethod("builder");
        assertTrue(Modifier.isStatic(builder.getModifiers()), "builder() must be static");
        Class<?> builderType = builder.getReturnType();
        assertTrue(runtime.getMethod("eval", String.class) != null);
        assertTrue(runtime.getMethod("load", Path.class) != null);
        assertTrue(builderType.getMethod("build") != null);
        assertTrue(AutoCloseable.class.isAssignableFrom(runtime),
                "ShenRuntime must be closeable");
    }

    @Test
    void runtimeCanEvaluateSimpleExpression() throws Exception {
        try (ShenRuntime runtime = ShenRuntime.builder().build()) {
            Value value = runtime.eval("(+ 40 2)");
            assertTrue(value.isNumber());
            assertTrue(value.fitsInLong());
            org.junit.jupiter.api.Assertions.assertEquals(42L, value.asLong());
        }
    }

    @Test
    void divisionPreservesExactIntegersAndTrapsZero() {
        try (ShenRuntime runtime = ShenRuntime.builder().standardLibrary(false).build()) {
            Value exact = runtime.eval("(/ 4 2)");
            assertEquals(2L, exact.asLong());
            assertEquals("2", exact.toString());
            assertEquals(2.5d, runtime.eval("(/ 5 2)").asDouble());
            assertEquals("divide-by-zero",
                    runtime.eval("(trap-error (/ 1 0) (lambda E divide-by-zero))").toString());
        }
    }

    @Test
    void runtimeCompilesShenSourceRatherThanOnlyKLambda() {
        try (ShenRuntime runtime = ShenRuntime.builder().standardLibrary(false).build()) {
            runtime.eval("(define answer X -> (+ X 1))");
            Value value = runtime.eval("(answer 41)");
            org.junit.jupiter.api.Assertions.assertEquals(42L, value.asLong());
        }
    }

    @Test
    void shakenRuntimeInitialisesBeforeUserFormsAndPreservesArgv(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("yggdrasil.manifest.txt"),
                "manifest-version=3\nkernel-version=42-s42.20260825\n" +
                "kernel=kernel.kl\ninit=shen.initialise\nuser=program.kl\n");
        Files.writeString(dir.resolve("kernel.kl"),
                "(defun shen.initialise () (set ready true))\n");
        Files.writeString(dir.resolve("program.kl"),
                "(if (= (value ready) true) " +
                "(write-byte (string->n (pos (hd (value *argv*)) 0)) (value *stoutput*)) " +
                "(simple-error \"not initialised\"))\n");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            ShakenSupport.run(dir, new String[]{"alpha", "beta"});
        } finally {
            System.setOut(previous);
        }
        assertEquals("a", output.toString(StandardCharsets.UTF_8));
    }
}
