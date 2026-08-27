package com.github.ragnard.shen.tests;

import org.junit.jupiter.api.Test;
import com.github.ragnard.shen.Shen;
import com.github.ragnard.shen.ShenRuntime;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end checks for the common Bifrost launcher contract. */
class CliContractTest {
    @Test
    void versionAndHelpAreSuccessful() throws Exception {
        ProcessResult version = invoke("--version");
        assertEquals(0, version.status());
        assertTrue(version.output().contains("42.0"));
        ProcessResult help = invoke("--help");
        assertEquals(0, help.status());
        assertTrue(help.output().toLowerCase().contains("usage"));
    }

    @Test
    void evalPreservesExpressionOrder() throws Exception {
        ProcessResult result = invoke("eval", "-e", "(+ 40 2)");
        assertEquals(0, result.status());
        assertTrue(result.output().contains("42"));
    }

    private static ProcessResult invoke(String... args) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        synchronized (CliContractTest.class) {
            var oldOut = System.out;
            var oldErr = System.err;
            System.setOut(new java.io.PrintStream(output, true, StandardCharsets.UTF_8));
            System.setErr(new java.io.PrintStream(output, true, StandardCharsets.UTF_8));
            try (ShenRuntime runtime = ShenRuntime.builder().out(output).err(output).build()) {
                var method = Shen.class.getDeclaredMethod("run", ShenRuntime.class, String[].class);
                method.setAccessible(true);
                int status = (Integer) method.invoke(null, runtime, args);
                return new ProcessResult(status, output.toString(StandardCharsets.UTF_8));
            } finally {
                System.setOut(oldOut);
                System.setErr(oldErr);
            }
        }
    }

    private record ProcessResult(int status, String output) {}
}
