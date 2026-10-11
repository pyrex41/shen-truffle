package com.github.ragnard.shen.klambda.nodes.builtins.arithmetic;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.Fallback;
import com.github.ragnard.shen.klambda.nodes.TrapException;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "-")
public abstract class Subtract extends BuiltinNode {
    @Fallback
    protected Object invalidOperands(Object x, Object y) {
        throw new TrapException("Subtract: numeric operands required");
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    public long subtractLongs(long x, long y) {
        return Math.subtractExact(x, y);
    }

    /** Integers beyond 64 bits continue as floats instead of failing the host. */
    @Specialization(replaces = "subtractLongs")
    public Object subtractLongsOverflowing(long x, long y) {
        try {
            return Math.subtractExact(x, y);
        } catch (ArithmeticException e) {
            return (double) x - (double) y;
        }
    }

    @Specialization
    public double subtract(double x, double y) {
        return x - y;
    }
}
