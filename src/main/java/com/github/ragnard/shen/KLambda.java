package com.github.ragnard.shen;

import com.github.ragnard.shen.klambda.Language;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class KLambda implements AutoCloseable {

    private final Context engine;

    public KLambda() {
        this(System.in, System.out);
    }

    public KLambda(InputStream in, OutputStream out) {
        this.engine = Context.newBuilder(Language.ID)
                .in(in).out(out).allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false").build();
    }

    public Object eval(String s) {
        try {
            Source code = Source.newBuilder(Language.ID, s, "<eval>")
                    .interactive(true).build();
            return eval(code);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid source", e);
        }
    }

    public Object eval(InputStream s) throws IOException {
        StringBuilder contents = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(s);
        char[] buffer = new char[8192];
        int count;
        while ((count = reader.read(buffer)) != -1) contents.append(buffer, 0, count);
        Source code = Source.newBuilder(Language.ID, contents, "<eval>")
                .interactive(true)
                .build();

        return eval(code);
    }

    public Object eval(Source source) {
        try {
            Value result = engine.eval(source);
            if (result.isHostObject()) return result.asHostObject();
            if (result.isBoolean()) return result.asBoolean();
            if (result.isString()) return result.asString();
            if (result.isNumber()) {
                if (result.fitsInLong()) return result.asLong();
                return result.asDouble();
            }
            return result;

        } catch (UnsupportedSpecializationException e) {
            throw e;
            //throw new RuntimeException(e.getNode().getEncapsulatingSourceSection().toString(), e);
        }
    }

    @Override
    public void close() {
        engine.close();
    }

    public static void main(String[] args) throws IOException {
        KLambda kl = new KLambda();

        kl.eval(System.in);
    }
}
