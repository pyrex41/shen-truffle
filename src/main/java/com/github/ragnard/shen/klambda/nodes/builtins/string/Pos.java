package com.github.ragnard.shen.klambda.nodes.builtins.string;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.github.ragnard.shen.klambda.nodes.TrapException;

@NodeInfo(shortName = "pos")
public abstract class Pos extends BuiltinNode {
    @Specialization
    public String pos(String string, long pos) {
        if (pos < 0 || pos >= string.codePointCount(0, string.length()))
            throw new TrapException("pos: index out of range: " + pos);
        int start = string.offsetByCodePoints(0, (int) pos);
        int end = string.offsetByCodePoints(start, 1);
        return string.substring(start, end);
    }
}
