package com.github.ragnard.shen.klambda.nodes.builtins.arithmetic;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Maps an integral float to the equal integer, leaving every other value
 * unchanged. The port's {@code hash} override uses it so that values which
 * are {@code =} (for example {@code 1} and {@code 1.0}) hash alike; the
 * kernel's {@code hash} works on {@code (str X)}, where they differ.
 */
@NodeInfo(shortName = "shen.truffle.normalise-number")
public abstract class NormaliseNumber extends BuiltinNode {
    @Specialization
    public long normalise(long x) {
        return x;
    }

    @Specialization
    public Object normalise(double x) {
        if (x == Math.rint(x) && x >= -0x1p63 && x < 0x1p63) return (long) x;
        return x;
    }

    @Fallback
    public Object other(Object x) {
        return x;
    }
}
