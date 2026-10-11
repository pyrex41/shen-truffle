package com.github.ragnard.shen.klambda.nodes;

import com.github.ragnard.shen.klambda.Context;
import com.github.ragnard.shen.klambda.Language;
import com.github.ragnard.shen.klambda.runtime.Function;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeChild("functionNode")
public abstract class FunctionExpressionNode extends ExpressionNode {

    @Specialization
    public Function doFunction(Function function) {
        return function;
    }

    @Specialization(assumptions = "cachedFunction.assumption")
    public Function doSymbol(Symbol functionName,
                             @Cached("resolveFunction(functionName)") Context.RegisteredFunction cachedFunction) {
        return cachedFunction.function;
    }

    /** Applying a non-function is an ordinary, trappable Shen error. */
    @Fallback
    public Function doNotAFunction(Object value) {
        throw new TrapException("attempt to apply non-function " + value);
    }

    protected Context.RegisteredFunction resolveFunction(Symbol functionName) {
        Context context = Language.CONTEXT_REFERENCE.get(this);
        Context.RegisteredFunction function = context.lookupFunction(functionName.getName());
        if (function == null) {
            throw new TrapException("fn: " + functionName + " is undefined");
        }
        return function;
    }

}
