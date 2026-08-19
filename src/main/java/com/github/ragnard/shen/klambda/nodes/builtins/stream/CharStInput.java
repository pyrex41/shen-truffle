package com.github.ragnard.shen.klambda.nodes.builtins.stream;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import java.io.Reader;

/** True for character input streams (used by Shen's reader dispatch). */
@NodeInfo(shortName = "shen.char-stinput?")
public abstract class CharStInput extends BuiltinNode {
    @Specialization
    public Symbol isCharInput(Object stream) {
        return Symbol.fromBoolean(stream instanceof Reader);
    }
}
