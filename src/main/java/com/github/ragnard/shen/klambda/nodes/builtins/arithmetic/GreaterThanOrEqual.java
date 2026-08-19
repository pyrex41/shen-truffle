package com.github.ragnard.shen.klambda.nodes.builtins.arithmetic;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.Fallback;
import com.github.ragnard.shen.klambda.nodes.TrapException;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = ">=")
public abstract class GreaterThanOrEqual extends BuiltinNode {
    @Fallback
    protected Object invalidOperands(Object x, Object y) {
        throw new TrapException("GreaterThanOrEqual: numeric operands required");
    }
    @Specialization
    public Symbol greaterThanOrEqual(long x, long y) {
        return Symbol.fromBoolean(x >= y);
    }

    @Specialization
    public Symbol greaterThanOrEqual(double x, double y) {
        return Symbol.fromBoolean(x >= y);
    }

}
