package com.github.ragnard.shen.klambda.nodes.builtins.stream;

import com.github.ragnard.shen.klambda.nodes.TrapException;
import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.io.IOException;
import java.io.Writer;

/** Writes and flushes a string to a host character stream, returning the string. */
@NodeInfo(shortName = "write-string")
public abstract class WriteString extends BuiltinNode {
    @Specialization
    public String writeString(String value, Writer stream) {
        try {
            stream.write(value);
            stream.flush();
            return value;
        } catch (IOException e) {
            throw new TrapException("write-string: " + e.getMessage());
        }
    }
}
