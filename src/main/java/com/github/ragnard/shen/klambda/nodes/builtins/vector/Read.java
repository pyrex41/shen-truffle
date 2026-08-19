package com.github.ragnard.shen.klambda.nodes.builtins.vector;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.github.ragnard.shen.klambda.runtime.Cons;
import com.github.ragnard.shen.klambda.runtime.Vector;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.github.ragnard.shen.klambda.nodes.TrapException;

@NodeInfo(shortName = "<-address")
public abstract class Read extends BuiltinNode {
    @Specialization
    public Object read(Vector vector, long pos) {
        if (pos < 0 || pos >= vector.size()) throw new TrapException("<-address: index out of range: " + pos);
        Object v = vector.get((int) pos);
        return v != null ? v : Cons.EMPTY;
    }
    @Specialization
    public Object read(Object[] vector, long pos) {
        if (pos < 0 || pos >= vector.length)
            throw new TrapException("<-address: index out of range: " + pos);
        Object v = vector[(int)pos];
        return v != null ? v : Cons.EMPTY; //Symbol.intern("fail!");
    }
}
