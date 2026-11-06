package com.github.ragnard.shen.klambda.nodes.builtins.host;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.interop.InteropLibrary;

/**
 * Ends the guest program with a status. Not a Shen error, so {@code trap-error}
 * lets it through; the embedder sees a polyglot exit with this status.
 */
@NodeInfo(shortName = "shen.truffle.exit")
public abstract class Exit extends BuiltinNode {
    @Specialization
    public Object exit(long status) {
        throw new ExitException(status);
    }

    @ExportLibrary(InteropLibrary.class)
    static final class ExitException extends AbstractTruffleException {
        private static final long serialVersionUID = 1L;
        private final int status;
        ExitException(long status) { this.status = (int) status; }
        @ExportMessage ExceptionType getExceptionType() { return ExceptionType.EXIT; }
        @ExportMessage int getExceptionExitStatus() { return status; }
    }
}
