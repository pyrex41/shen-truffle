package com.github.ragnard.shen.klambda.nodes.builtins.stream;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import java.io.Writer;

/** True for character output streams (used by Shen's writer dispatch). */
@NodeInfo(shortName = "shen.char-stoutput?")
public abstract class CharStOutput extends BuiltinNode {
    @Specialization
    public Symbol isCharOutput(Object stream) {
        return Symbol.fromBoolean(stream instanceof Writer);
    }
}
