package com.github.ragnard.shen.klambda.nodes.builtins.string;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.github.ragnard.shen.klambda.nodes.TrapException;

@NodeInfo(shortName = "n->string")
public abstract class NumberToString extends BuiltinNode {

    @Specialization
    public String numberToString(long n) {
        if (n < Character.MIN_CODE_POINT || n > Character.MAX_CODE_POINT ||
                Character.isSurrogate((char) n)) {
            throw new TrapException("n->string: invalid code point: " + n);
        }
        return new String(Character.toChars((int) n));
    }
}
