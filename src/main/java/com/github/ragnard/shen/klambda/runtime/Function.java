package com.github.ragnard.shen.klambda.runtime;

import com.github.ragnard.shen.klambda.Language;
import com.github.ragnard.shen.klambda.nodes.ExpressionNode;
import com.github.ragnard.shen.klambda.nodes.RootNode;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class Function implements TruffleObject {

    private final RootCallTarget callTarget;

    private final int arity;

    private final Object[] curriedArgumentValues;

    private MaterializedFrame lexicalScope;

    private String name;
    private Object form;

    public Function(RootCallTarget callTarget, int arity) {
        this(callTarget, arity, new Object[0]);
    }

    public Function(RootCallTarget callTarget, int arity, Object[] curriedArgumentValues) {
        this.callTarget = callTarget;
        this.arity = arity;
        this.curriedArgumentValues = curriedArgumentValues;
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    public int getArity() {
        return arity;
    }

    public MaterializedFrame getLexicalScope() {
        return lexicalScope;
    }

    public String getName() {
        return this.name;
    }

    public Function setName(String name) {
        this.name = name;
        return this;
    }

    public Function setLexicalClosure(MaterializedFrame lexicalScope) {
        //this.lexicalScope = lexicalScope;
        //return this;
        Function x = new Function(this.callTarget, this.arity, this.curriedArgumentValues);
        x.lexicalScope = lexicalScope;
        x.form = this.form;
        return x;
    }

    public Object[] packArguments(Object[] arguments) {
        Object[] allArguments = new Object[1+this.curriedArgumentValues.length+arguments.length];
        allArguments[0] = this.lexicalScope;
        System.arraycopy(this.curriedArgumentValues, 0, allArguments, 1, this.curriedArgumentValues.length);
        System.arraycopy(arguments, 0, allArguments, 1+this.curriedArgumentValues.length, arguments.length);
        return allArguments;
    }

    public Function curry(Object[] argumentValues) {
        if (argumentValues.length == 0) {
            return this;
        }

        Object[] curriedArgumentValues = new Object[this.curriedArgumentValues.length + argumentValues.length];
        System.arraycopy(this.curriedArgumentValues, 0, curriedArgumentValues, 0, this.curriedArgumentValues.length);
        System.arraycopy(argumentValues, 0, curriedArgumentValues, this.curriedArgumentValues.length, argumentValues.length);

        Function f = new Function(
                this.callTarget,
                this.arity - argumentValues.length,
                curriedArgumentValues
        );
        f.form = this.form;
        f.name = this.name;
        return f;
    }

    public static Function create(Language language, FrameDescriptor frameDescriptor, int arity, ExpressionNode body, String name) {
        return new Function(new RootNode(language, frameDescriptor, body, name).getCallTarget(), arity);
    }

    public void setForm(Object form) {
        this.form = form;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return this.name == null ? "<function>" : "(fn " + this.name + ")";
    }

    @ExportMessage public boolean isExecutable() { return true; }

    @ExportMessage
    public Object execute(Object... arguments) {
        return callTarget.call(packArguments(arguments));
    }

    @ExportMessage @TruffleBoundary public String toDisplayString(boolean allowSideEffects) { return toString(); }
}
