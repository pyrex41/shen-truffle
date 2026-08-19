package com.github.ragnard.shen.klambda.nodes.builtins.stream;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.github.ragnard.shen.klambda.nodes.TrapException;

import java.io.IOException;
import java.io.OutputStream;

@NodeInfo(shortName = "write-byte")
public abstract class WriteByte extends BuiltinNode {
    @Specialization
    public long writeByte(long value, OutputStream stream) {
        if (value < 0 || value > 255) throw new TrapException("write-byte: invalid byte: " + value);
        try {
            stream.write((int) value);
            stream.flush();
            return value;
        } catch (IOException e) {
            throw new TrapException("write-byte: " + e.getMessage());
        }
    }
}
