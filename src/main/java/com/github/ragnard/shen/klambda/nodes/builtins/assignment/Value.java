package com.github.ragnard.shen.klambda.nodes.builtins.assignment;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.github.ragnard.shen.klambda.nodes.TrapException;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "value")
public abstract class Value extends BuiltinNode {

    @Specialization(guards = "symbol == cachedSymbol", limit = "2")
    public Object value(Symbol symbol,
                        @Cached("symbol") Symbol cachedSymbol,
                        @Cached("lookupFrameSlot(cachedSymbol)") int cachedFrameSlot) {
        return requireValue(symbol, this.getContext().getGlobalFrame().getValue(cachedFrameSlot));
    }

    @Specialization(replaces = "value")
    public Object valueSlow(Symbol symbol) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        int frameSlot = lookupFrameSlot(symbol);
        return requireValue(symbol, this.getContext().getGlobalFrame().getValue(frameSlot));
    }

    protected int lookupFrameSlot(Symbol symbol) {
        return this.getContext().globalSlot(symbol.getName());
    }

    private static Object requireValue(Symbol symbol, Object value) {
        if (value == null) throw new TrapException("value: not set: " + symbol);
        return value;
    }
}
