package com.github.ragnard.shen.tests;

import com.github.ragnard.shen.ShenRuntime;
import com.github.ragnard.shen.klambda.Context;
import com.github.ragnard.shen.klambda.nodes.builtins.stream.CharStream;
import com.github.ragnard.shen.klambda.nodes.builtins.stream.ReadUnitString;
import com.github.ragnard.shen.klambda.nodes.builtins.stream.WriteString;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import org.junit.jupiter.api.Test;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Host-side contract for Shen 43.1's public character stream primitives. */
class CharacterStreamBuiltinTest {
    private static final Context NO_CONTEXT = null;

    @Test
    void readerUsesUtf16UnitsAndReturnsEmptyStringAtEof() throws IOException {
        ReadUnitString node = new ReadUnitString() {
            @Override public Context getContext() { return NO_CONTEXT; }
            @Override public Object executeGeneric(VirtualFrame frame) { return null; }
        };
        try (Reader reader = new StringReader("A\u0000\u03bb\ud83d\ude00")) {
            assertEquals("A", node.readUnitString(reader));
            assertEquals("\u0000", node.readUnitString(reader));
            assertEquals("λ", node.readUnitString(reader));
            assertEquals("\ud83d", node.readUnitString(reader));
            assertEquals("\ude00", node.readUnitString(reader));
            assertEquals("", node.readUnitString(reader));
        }
    }

    @Test
    void writerFlushesAndReturnsOriginalString() {
        WriteString node = new WriteString() {
            @Override public Context getContext() { return NO_CONTEXT; }
            @Override public Object executeGeneric(VirtualFrame frame) { return null; }
        };
        TrackingWriter writer = new TrackingWriter();
        String value = "A\u0000λ😀";
        assertEquals(value, node.writeString(value, writer));
        assertEquals(value, writer.contents);
        assertTrue(writer.flushed);
    }

    @Test
    void charStreamRecognisesOnlyReaderAndWriter() {
        CharStream node = new CharStream() {
            @Override public Context getContext() { return NO_CONTEXT; }
            @Override public Object executeGeneric(VirtualFrame frame) { return null; }
        };
        assertEquals(Symbol.TRUE, node.isCharStream(new StringReader("x")));
        assertEquals(Symbol.TRUE, node.isCharStream(new TrackingWriter()));
        assertEquals(Symbol.FALSE, node.isCharStream(System.in));
        assertEquals(Symbol.FALSE, node.isCharStream(System.out));
    }

    @Test
    void byteStreamsRemainInvalidForCharacterOperationsAndErrorsAreCatchable() {
        try (ShenRuntime runtime = ShenRuntime.builder().standardLibrary(false).build()) {
            assertEquals("false", runtime.eval("(char-stream? (stinput))").toString());
            assertEquals("caught", runtime.eval(
                    "(trap-error (read-unit-string (stinput)) (lambda E caught))").toString());
            assertEquals("caught", runtime.eval(
                    "(trap-error (write-string \"x\" (stinput)) (lambda E caught))").toString());
            assertEquals(1L, runtime.eval("(arity char-stream?)").asLong());
            assertEquals(1L, runtime.eval("(arity read-unit-string)").asLong());
            assertEquals(2L, runtime.eval("(arity write-string)").asLong());
        }
    }

    private static final class TrackingWriter extends Writer {
        private final StringBuilder out = new StringBuilder();
        private String contents = "";
        private boolean flushed;

        @Override public void write(char[] cbuf, int off, int len) {
            out.append(cbuf, off, len);
            contents = out.toString();
        }
        @Override public void flush() { flushed = true; }
        @Override public void close() { }
    }
}
