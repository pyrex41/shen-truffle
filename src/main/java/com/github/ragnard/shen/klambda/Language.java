package com.github.ragnard.shen.klambda;

import com.github.ragnard.shen.klambda.nodes.RootNode;
import com.github.ragnard.shen.klambda.nodes.SequenceNode;
import com.github.ragnard.shen.util.IndexedPushbackReader;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.source.Source;

import java.util.ArrayList;
import java.util.List;

@TruffleLanguage.Registration(id = "klambda", name = "KLambda", version = "42", characterMimeTypes = Language.MIME_TYPE)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class})
public class Language extends TruffleLanguage<Context> {

    public static final String ID = "klambda";
    public static final ContextReference<Context> CONTEXT_REFERENCE = ContextReference.create(Language.class);

    public static final String MIME_TYPE = "application/x-klambda";

    public Language() {}

    @Override
    protected Context createContext(Env env) {
        return new Context(this, env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();

        try(IndexedPushbackReader r = new IndexedPushbackReader(source.getReader())) {
            List<CallTarget> callTargets = new ArrayList<>();

            while(true) {
                int startIndex = r.getPosition();

                Object form = Reader.read(r);

                if (form == null) {
                    break;
                }

                RootNode expr = Analyzer.analyzeRoot(this, form, source.createSection(startIndex, r.getPosition()-startIndex-1));
                callTargets.add(expr.getCallTarget());
            }

            SequenceNode node = new SequenceNode(callTargets.toArray(new CallTarget[0]));
            RootNode root = new RootNode(this, null, node);
            //NodeUtil.printCompactTree(System.out, root);

            return root.getCallTarget();
            //return Truffle.getRuntime().createCallTarget(new RootNode(null, null, doNode));
        }
    }

    protected boolean isObjectOfLanguage(Object object) {
        return false;
    }

}
