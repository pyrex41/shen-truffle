package com.github.ragnard.shen.klambda.nodes.builtins.string;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.github.ragnard.shen.klambda.nodes.TrapException;

@NodeInfo(shortName = "string->n")
public abstract class StringToNumber extends BuiltinNode {

    @Specialization
    public long stringToNumber(String s) {
        if (s.codePointCount(0, s.length()) != 1) {
            throw new TrapException("string->n: not a unit string: " + s);
        }
        return s.codePointAt(0);
    }
}
