package com.github.ragnard.shen.klambda.nodes.builtins.stream;

import com.github.ragnard.shen.klambda.nodes.builtins.BuiltinNode;
import com.github.ragnard.shen.klambda.runtime.Symbol;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.github.ragnard.shen.klambda.nodes.TrapException;

import java.io.*;

@NodeInfo(shortName = "open")
public abstract class Open extends BuiltinNode {
    @Specialization
    @CompilerDirectives.TruffleBoundary
    public Closeable open(String path, Symbol direction) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            MaterializedFrame globals = this.getContext().getGlobalFrame();
            int homeDirectorySlot = this.getContext().globalSlot("*home-directory*");
            String homeDirectory = (String) globals.getValue(homeDirectorySlot);
            file = new File(homeDirectory, path);
            //throw new RuntimeException("not implemented");
        }

        //System.out.println(file.getAbsoluteFile());

        try {
            switch (direction.getName()) {
                case "in":
                    return new BufferedInputStream(new FileInputStream(file));
                case "out":
                    return new BufferedOutputStream(new FileOutputStream(file));
            }
        } catch (FileNotFoundException e) {
            throw new TrapException("open: file not found: " + file);
        }

        throw new TrapException("open: invalid direction");
    }
}
