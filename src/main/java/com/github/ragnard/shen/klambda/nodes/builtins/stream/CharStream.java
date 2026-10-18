package com.github.ragnard.shen.klambda.nodes.builtins.stream;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.io.Reader;
import java.io.Writer;

/** True for host character input or output streams. */
@NodeInfo(shortName = "char-stream?")
public abstract class CharStream extends BuiltinNode {
    @Specialization
    public Symbol isCharStream(Object stream) {
        return Symbol.fromBoolean(stream instanceof Reader || stream instanceof Writer);
    }
}
