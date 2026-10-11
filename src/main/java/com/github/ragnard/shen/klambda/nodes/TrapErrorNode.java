package com.github.ragnard.shen.klambda.nodes;

import com.github.ragnard.shen.klambda.runtime.Function;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;

public class TrapErrorNode extends ExpressionNode {

    @Child
    private ExpressionNode bodyNode;

    @Child
    private ExpressionNode handlerNode;

    @Child
    private DispatchNode dispatchNode;

    public TrapErrorNode(ExpressionNode bodyNode, ExpressionNode handlerNode) {
        this.bodyNode = bodyNode;
        this.handlerNode = handlerNode;
        this.dispatchNode = DispatchNodeGen.create();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            return this.bodyNode.executeGeneric(frame);
        } catch (TrapException e) {
            return handle(frame, e);
        } catch (UnsupportedSpecializationException e) {
            // A primitive applied to arguments of the wrong type is an ordinary Shen error.
            return handle(frame, new TrapException(primitiveTypeError(e)));
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static String primitiveTypeError(UnsupportedSpecializationException e) {
        String name = "primitive";
        for (Class<?> c = e.getNode() == null ? null : e.getNode().getClass(); c != null; c = c.getSuperclass()) {
            NodeInfo info = c.getAnnotation(NodeInfo.class);
            if (info != null && !info.shortName().isEmpty()) { name = info.shortName(); break; }
        }
        StringBuilder args = new StringBuilder();
        for (Object v : e.getSuppliedValues()) args.append(' ').append(v);
        return name + ": unexpected argument" + (e.getSuppliedValues().length == 1 ? "" : "s") + args;
    }

    private Object handle(VirtualFrame frame, TrapException e) {
        Function handler = (Function) this.handlerNode.executeGeneric(frame);
        return call(frame, handler.getCallTarget(), new Object[]{handler.getLexicalScope(), e});
    }

    protected Object call(VirtualFrame frame, CallTarget callTarget, Object[] arguments) {
        if (this.isTail()) {
            throw new TailCallException(callTarget, arguments);
        }
        while (true) {
            try {
                return dispatchNode.executeDispatch(frame, callTarget, arguments);
                //return callTarget.call(arguments);
            } catch (TailCallException e) {
                callTarget = e.callTarget;
                arguments = e.arguments;
            }
        }
    }

    @Override
    public void setIsTail() {
        super.setIsTail();
        //this.bodyNode.setIsTail();
        this.handlerNode.setIsTail();
    }
}
