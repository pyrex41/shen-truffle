package com.github.ragnard.shen.klambda.nodes.builtins.stream;

import com.github.ragnard.shen.klambda.nodes.TrapException;
import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.io.IOException;
import java.io.Reader;

/** Reads one UTF-16 code unit from a host character stream, or an empty string at EOF. */
@NodeInfo(shortName = "read-unit-string")
public abstract class ReadUnitString extends BuiltinNode {
    @Specialization
    public String readUnitString(Reader stream) {
        try {
            int c = stream.read();
            return c < 0 ? "" : String.valueOf((char) c);
        } catch (IOException e) {
            throw new TrapException("read-unit-string: " + e.getMessage());
        }
    }
}
