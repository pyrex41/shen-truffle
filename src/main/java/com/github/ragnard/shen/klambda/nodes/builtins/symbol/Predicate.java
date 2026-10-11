package com.github.ragnard.shen.klambda.nodes.builtins.symbol;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Host representation check used by the port's {@code symbol?} override.
 * The portable kernel definition decides by parsing {@code (str X)}, which
 * misclassifies host values whose printed form happens to be valid symbol
 * syntax (an anonymous function prints as {@code <function>}).
 */
@NodeInfo(shortName = "shen.truffle.symbol-object?")
public abstract class Predicate extends BuiltinNode {
    @Specialization
    public Symbol isSymbol(Object x) {
        return Symbol.fromBoolean(x instanceof Symbol);
    }
}
