package com.github.ragnard.shen.klambda.nodes.builtins.string;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.github.ragnard.shen.klambda.nodes.TrapException;

@NodeInfo(shortName = "tlstr")
public abstract class Tail extends BuiltinNode {
    @Specialization
    public String tail(String s) {
        if (s.isEmpty()) {
            throw new TrapException("tlstr: too short: " + s);
        }
        return s.substring(s.offsetByCodePoints(0, 1));
    }
}
