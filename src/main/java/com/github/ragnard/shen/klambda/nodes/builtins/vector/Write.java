package com.github.ragnard.shen.klambda.nodes.builtins.vector;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.github.ragnard.shen.klambda.nodes.TrapException;
import com.github.ragnard.shen.klambda.runtime.Vector;

@NodeInfo(shortName = "address->")
public abstract class Write extends BuiltinNode {
    @Specialization
    public Object write(Vector vector, long pos, Object value) {
        if (pos < 0 || pos >= vector.size()) throw new TrapException("address->: index out of range: " + pos);
        vector.set((int) pos, value);
        return vector;
    }
    @Specialization
    public Object write(Object[] vector, long pos, Object value) {
        if (pos < 0 || pos >= vector.length)
            throw new TrapException("address->: index out of range: " + pos);
        vector[(int)pos] = value;
        return vector;
    }
}
