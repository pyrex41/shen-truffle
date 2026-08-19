package com.github.ragnard.shen.klambda.nodes.local;

import com.github.ragnard.shen.klambda.nodes.ExpressionNode;
import com.oracle.truffle.api.dsl.NodeFields;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

@NodeFields({@NodeField(name = "slot", type = int.class), @NodeField(name = "depth", type = int.class)})
public abstract class ReadLexicalClosureVariableNode extends ExpressionNode {
    public abstract int getSlot();
    public abstract int getDepth();

    @ExplodeLoop
    protected Frame readUpStack(Frame frame) {
        Frame lookup = frame;
        for (int i = 0; i < getDepth(); i++) lookup = getLexicalClosure(lookup);
        return lookup;
    }

    @Specialization
    protected Object read(VirtualFrame frame) {
        return readUpStack(frame).getValue(getSlot());
    }
}
