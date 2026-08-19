package com.github.ragnard.shen.klambda.runtime;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/** Mutable Shen abstract vector with Polyglot array interop. */
@ExportLibrary(InteropLibrary.class)
public final class Vector implements TruffleObject {
    private final Object[] values;
    public Vector(int size) { values = new Object[size]; }
    public Vector(Object[] values) { this.values = values; }
    public int size() { return values.length; }
    public Object get(int index) { return values[index]; }
    public void set(int index, Object value) { values[index] = value; }
    public Object[] values() { return values; }
    @ExportMessage public boolean hasArrayElements() { return true; }
    @ExportMessage public long getArraySize() { return values.length; }
    @ExportMessage public boolean isArrayElementReadable(long index) { return index >= 0 && index < values.length; }
    @ExportMessage public Object readArrayElement(long index) throws InvalidArrayIndexException {
        if (index < 0 || index >= values.length) throw InvalidArrayIndexException.create(index);
        return values[(int) index];
    }
    @ExportMessage public boolean isArrayElementModifiable(long index) { return index >= 0 && index < values.length; }
    @ExportMessage public boolean isArrayElementInsertable(long index) { return false; }
    @ExportMessage public void writeArrayElement(long index, Object value) throws InvalidArrayIndexException {
        if (index < 0 || index >= values.length) throw InvalidArrayIndexException.create(index);
        values[(int) index] = value;
    }
    @ExportMessage public String toDisplayString(boolean allowSideEffects) { return java.util.Arrays.toString(values); }
}
