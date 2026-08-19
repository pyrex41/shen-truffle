package com.github.ragnard.shen.klambda.nodes.builtins.vector;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.github.ragnard.shen.klambda.nodes.TrapException;
import com.github.ragnard.shen.klambda.runtime.Vector;

@NodeInfo(shortName = "absvector")
public abstract class Create extends BuiltinNode {
    @Specialization
    public Vector create(long size) {
        if (size < 0 || size > Integer.MAX_VALUE)
            throw new TrapException("absvector: invalid size: " + size);
        return new Vector((int)size);
    }
}
