package com.github.ragnard.shen.klambda.nodes.builtins.arithmetic;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.Fallback;
import com.github.ragnard.shen.klambda.nodes.TrapException;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "+")
public abstract class Add extends BuiltinNode {
    @Fallback
    protected Object invalidOperands(Object x, Object y) {
        throw new TrapException("Add: numeric operands required");
    }
    @Specialization
    public long add(long x, long y) {
        return Math.addExact(x, y);
    }

    @Specialization
    public double add(double x, double y) {
        return x + y;
    }

}
