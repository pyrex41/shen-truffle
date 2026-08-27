package com.github.ragnard.shen.klambda.nodes.builtins.string;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.github.ragnard.shen.klambda.runtime.Function;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import com.github.ragnard.shen.klambda.runtime.Vector;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.nodes.NodeInfo;
import java.util.Arrays;

@NodeInfo(shortName = "str")
public abstract class Str extends BuiltinNode {
    
    @Specialization
    @CompilerDirectives.TruffleBoundary
    public String str(long n) {
        return Long.toString(n);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    public String str(double n) {
        return Double.toString(n);
    }

    @Specialization
    public String str(String s) {
        return s;
    }

    @Specialization
    public String str(Symbol symbol) {
        return symbol.getName();
    }

    /**
     * Shen's vectors are represented by Object[] at the Java boundary.  They
     * are still valid values for {@code str} (notably in hashkey and error
     * formatting paths); leaving this specialization out causes an
     * UnsupportedSpecializationException while booting the 42.0 kernel.
     */
    @Specialization
    @CompilerDirectives.TruffleBoundary
    public String str(Object[] vector) {
        return Arrays.deepToString(vector);
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    public String str(Vector vector) {
        return Arrays.deepToString(vector.values());
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    public String str(Function f) {
        return f.toString();
    }

    /**
     * Compound and host-backed Shen values are printable too.  In particular,
     * the 42.0 kernel calls {@code str} while hashing type signatures (lists)
     * and vectors during its bootstrap.
     */
    @Fallback
    @CompilerDirectives.TruffleBoundary
    public String str(Object value) {
        return String.valueOf(value);
    }
}
