package com.github.ragnard.shen.klambda.nodes.builtins.assignment;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "set")
public abstract class Set extends BuiltinNode {

    @Specialization(guards = "symbol == cachedSymbol", limit = "2")
    public Object setFast(Symbol symbol, Object value,
                        @Cached("symbol") Symbol cachedSymbol,
                        @Cached("lookupFrameSlot(cachedSymbol)") int cachedFrameSlot,
                        @Cached("getGlobalFrame()") MaterializedFrame cachedGlobalFrame) {
        cachedGlobalFrame.setObject(cachedFrameSlot, value);
        return value;
    }

    @Specialization(replaces = "setFast")
    public Object setSlow(Symbol symbol, Object value) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        int frameSlot = lookupFrameSlot(symbol);
        this.getGlobalFrame().setObject(frameSlot, value);
        return value;
    }

    protected MaterializedFrame getGlobalFrame() {
        return this.getContext().getGlobalFrame();
    }

    protected int lookupFrameSlot(Symbol symbol) {
        return this.getContext().globalSlot(symbol.getName());
    }
}
