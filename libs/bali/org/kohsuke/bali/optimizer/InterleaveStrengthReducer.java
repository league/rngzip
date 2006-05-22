package org.kohsuke.bali.optimizer;

import java.util.HashSet;
import java.util.Set;

import com.sun.msv.grammar.AttributeExp;
import com.sun.msv.grammar.DataExp;
import com.sun.msv.grammar.ElementExp;
import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.ExpressionCloner;
import com.sun.msv.grammar.ExpressionPool;
import com.sun.msv.grammar.Grammar;
import com.sun.msv.grammar.InterleaveExp;
import com.sun.msv.grammar.ListExp;
import com.sun.msv.grammar.MixedExp;
import com.sun.msv.grammar.OtherExp;
import com.sun.msv.grammar.ReferenceExp;
import com.sun.msv.grammar.ValueExp;
import com.sun.msv.grammar.trex.TREXGrammar;
import com.sun.msv.grammar.util.ExpressionFinder;

/**
 * Replaces interleaves whose branch is attribute by groups.
 * 
 * Groups are computationally less expensive than interleaves,
 * so this is expected to reduce the size of the grammar.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class InterleaveStrengthReducer extends ExpressionCloner {

    private InterleaveStrengthReducer( ExpressionPool pool ) { super(pool); }
    
    public static Grammar optimize( Grammar grammar ) {
        ExpressionPool pool = grammar.getPool();
        TREXGrammar result = new TREXGrammar(pool);
        InterleaveStrengthReducer r = new InterleaveStrengthReducer(pool);
        result.exp = grammar.getTopLevel().visit(r);
        return result;
    }
    
    
    private final Set visitedExps = new HashSet();
    
    public Expression onAttribute(AttributeExp att) {
        return att;
    }

    public Expression onElement(ElementExp exp) {
        if( visitedExps.add(exp) )
            exp.contentModel = exp.contentModel.visit(this);
        return exp;
    }

    public Expression onInterleave(InterleaveExp exp) {
        if( !exp.exp1.visit(hasElementOrText)
        ||  !exp.exp2.visit(hasElementOrText) )
            return pool.createSequence( exp.exp1.visit(this), exp.exp2.visit(this) );
        else
            return super.onInterleave(exp);
    }

    public Expression onRef(ReferenceExp exp) {
        if( visitedExps.add(exp) )
            exp.exp = exp.exp.visit(this);
        return exp;
    }

    public Expression onOther(OtherExp exp) {
        return exp.exp.visit(this);
    }

    /**
     * Returns true if the expression contains elements or texts.
     */
    private static ExpressionFinder hasElementOrText = new ExpressionFinder() {
        public boolean onAnyString() { return true; }
        public boolean onAttribute(AttributeExp exp) { return false; }
        public boolean onData(DataExp exp) { return true; }
        public boolean onElement(ElementExp exp) { return true; }
        public boolean onList(ListExp exp) { return true; }
        public boolean onMixed(MixedExp exp) { return true; }
        public boolean onValue(ValueExp exp) { return true; }
    };
}
