package com.github.ragnard.shen.klambda.nodes.builtins.arithmetic;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.Fallback;
import com.github.ragnard.shen.klambda.nodes.TrapException;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "/")
public abstract class Divide extends BuiltinNode {
    @Fallback
    protected Object invalidOperands(Object x, Object y) {
        throw new TrapException("Divide: numeric operands required");
    }
    /** Preserve Shen's exact integer result when the quotient is integral.
     *  Non-integral integer division promotes to a double, matching the
     *  numeric tower while avoiding the historical 2.0 result for 4 / 2. */
    @Specialization
    public Object divide(long x, long y) {
        if (y == 0) throw new TrapException("Divide: division by zero");
        if (x == Long.MIN_VALUE && y == -1)
            throw new TrapException("Divide: integer overflow");
        long quotient = x / y;
        long remainder = x % y;
        // Keep these as separate returns: a conditional expression containing
        // Long and Double operands is subject to Java numeric promotion and
        // would silently box both branches as Double.
        if (remainder == 0) return Long.valueOf(quotient);
        return Double.valueOf(((double) x) / y);
    }

    @Specialization
    public double divide(double x, double y) {
        if (y == 0.0) throw new TrapException("Divide: division by zero");
        return x / y;
    }
}
