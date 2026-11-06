package com.github.ragnard.shen.tests;

import com.github.ragnard.shen.ShenRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The REPL's own read exits cleanly at EOF; programs it evaluates see ordinary EOF. */
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class ReplEofTest {
    private record Result(int status, String output) {}

    private static Result repl(String input) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ShenRuntime rt = ShenRuntime.builder()
                .in(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)))
                .out(out).err(out).build()) {
            int status = rt.repl();
            return new Result(status, out.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    void emptyInputExitsCleanly() {
        Result r = repl("");
        assertEquals(0, r.status());
        assertFalse(r.output().contains("empty stream"));
    }

    @Test
    void finalFormWithoutNewlineRuns() {
        Result r = repl("(+ 40 2)");
        assertEquals(0, r.status());
        assertTrue(r.output().contains("42"), r.output());
    }

    @Test
    void multiLineFormRuns() {
        Result r = repl("(+ 40\n2)");
        assertEquals(0, r.status());
        assertTrue(r.output().contains("42"), r.output());
    }

    @Test
    void unterminatedFormDoesNotHang() {
        assertEquals(0, repl("(+ 1").status());
    }

    @Test
    void programReadByteSeesMinusOne() {
        Result r = repl("(read-byte (stinput))\n");
        assertEquals(0, r.status());
        assertTrue(r.output().contains("-1"), r.output());
    }

    @Test
    void programReadReportsEmptyStreamAndReplExitsAtNextPrompt() {
        Result r = repl("(read (stinput))\n");
        assertEquals(0, r.status());
        assertTrue(r.output().contains("empty stream"), r.output());
    }
}
