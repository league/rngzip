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
import com.sun.msv.grammar.ListExp;
import com.sun.msv.grammar.MixedExp;
import com.sun.msv.grammar.OtherExp;
import com.sun.msv.grammar.ReferenceExp;
import com.sun.msv.grammar.SequenceExp;
import com.sun.msv.grammar.ValueExp;
import com.sun.msv.grammar.trex.TREXGrammar;
import com.sun.msv.grammar.util.ExpressionFinder;

/**
 * Re-orders AttributeExps inside SequenceExps so that
 * attribute comes earlier in the expression
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AttributeReorder extends ExpressionCloner {
    
    private final Set visitedExps = new HashSet();
    
    /** Returns true if an expression contains something other than attributes. */
    private static final ExpressionFinder hasNonAttributes = new ExpressionFinder() {
        public boolean onAnyString()                {  return true; }
        public boolean onAttribute(AttributeExp exp){ return false; }
        public boolean onData(DataExp exp)          { return true; }
        public boolean onElement(ElementExp exp)    { return true; }
        public boolean onList(ListExp exp)          { return true; }
        public boolean onMixed(MixedExp exp)        { return true; }
        public boolean onValue(ValueExp exp)        { return true; }
    };
    
    
    public static Grammar optimize( Grammar g ) {
        ExpressionPool newPool = new ExpressionPool();
        TREXGrammar result = new TREXGrammar(newPool);
        AttributeReorder u = new AttributeReorder(newPool);
        result.exp = g.getTopLevel().visit(u);
        return result;
    }
    

    private AttributeReorder(ExpressionPool pool) {
        super(pool);
    }

    public Expression onSequence(SequenceExp exp) {
        Expression[] children = exp.getChildren();
        
        // process branches first
        for( int i=0; i<children.length; i++ )
            children[i] = children[i].visit(this);
        
        Expression r = Expression.epsilon;
        
        // combine attributes
        for( int i=0; i<children.length; i++ ) {
            if( !children[i].visit(hasNonAttributes) ) {
                r = pool.createSequence(r,children[i]);
                children[i] = null;
            }
        }
        // followed by other branches
        for( int i=0; i<children.length; i++ )
            if( children[i]!=null )
                r = pool.createSequence(r,children[i]);
        
        return r;
    }

    public Expression onAttribute(AttributeExp exp) {
        return exp;
    }

    public Expression onElement(ElementExp exp) {
        if( visitedExps.add(exp) )
            exp.contentModel = exp.contentModel.visit(this);
        return exp;
    }

    public Expression onRef(ReferenceExp exp) {
        return exp.exp.visit(this);
    }

    public Expression onOther(OtherExp exp) {
        return exp.exp.visit(this);
    }

}
