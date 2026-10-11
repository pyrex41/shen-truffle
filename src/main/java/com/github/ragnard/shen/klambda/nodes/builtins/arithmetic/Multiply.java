package com.github.ragnard.shen.klambda.nodes.builtins.arithmetic;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.Fallback;
import com.github.ragnard.shen.klambda.nodes.TrapException;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "*")
public abstract class Multiply extends BuiltinNode {
    @Fallback
    protected Object invalidOperands(Object x, Object y) {
        throw new TrapException("Multiply: numeric operands required");
    }
    @Specialization(rewriteOn = ArithmeticException.class)
    public long multiplyLongs(long x, long y) {
        return Math.multiplyExact(x, y);
    }

    /** Integers beyond 64 bits continue as floats instead of failing the host. */
    @Specialization(replaces = "multiplyLongs")
    public Object multiplyLongsOverflowing(long x, long y) {
        try {
            return Math.multiplyExact(x, y);
        } catch (ArithmeticException e) {
            return (double) x * (double) y;
        }
    }

    @Specialization
    public double multiply(double x, double y) {
        return x * y;
    }
}
