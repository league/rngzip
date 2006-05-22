package org.kohsuke.bali.automaton.builder;

import com.sun.msv.grammar.*;
import com.sun.msv.grammar.util.NameClassSimplifier;

/**
 * Computes nullability of a regular expression.
 * 
 * This code is different from <code>Expression.isEpsilonReducible</code>
 * in the handling of non-existent attribute transition.
 * 
 * Whereas this code treats it as an alphabet (thus non-nullable),
 * <code>isEpsilonReducible</code> doesn't treat it as an alphabet.
 * 
 * For example, consider "@a?".
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NullabilityChecker implements ExpressionVisitorBoolean {

    public NullabilityChecker( boolean optimizeIgnorableAttribute ) {
        this.optimizeIgnorableAttribute = optimizeIgnorableAttribute;
    }
    
    
    private boolean optimizeIgnorableAttribute;

    // nullable primitives
    public boolean onAnyString()                    { return true; }
    public boolean onEpsilon()                      { return true; }
    
    // non-nullable primitives
    public boolean onAttribute(AttributeExp exp)    { return false; }
    public boolean onData(DataExp exp)              { return false; }
    public boolean onElement(ElementExp exp)        { return false; }
    public boolean onNullSet()                      { return false; }
    public boolean onValue(ValueExp exp)            { return false; }

    public boolean onChoice(ChoiceExp exp) {

        if( optimizeIgnorableAttribute && Util.isIgnorableOptionalAttribute(exp) )
            // we are going to ignore this part so treat it as nullable
            return true;

        // treat non-existent attribute transtions as non-nullable
        
        NameClass nc1 = AttNameCombiner.collect(exp.exp1);
        NameClass nc2 = AttNameCombiner.collect(exp.exp2);
        
        NameClass neg1 = NameClassSimplifier.simplify(new DifferenceNameClass(nc2,nc1));
        NameClass neg2 = NameClassSimplifier.simplify(new DifferenceNameClass(nc1,nc2));
        
        return ( exp.exp1.isEpsilonReducible() && neg1.isNull() )
            || ( exp.exp2.isEpsilonReducible() && neg2.isNull() );
    }

    public boolean onInterleave(InterleaveExp exp) {
        return exp.exp1.visit(this) && exp.exp2.visit(this);
    }
    public boolean onList(ListExp exp) {
        return exp.exp.visit(this);
    }
    public boolean onMixed(MixedExp exp) {
        return exp.exp.visit(this);
    }
    public boolean onOneOrMore(OneOrMoreExp exp) {
        return exp.exp.visit(this);
    }
    public boolean onOther(OtherExp exp) {
        return exp.exp.visit(this);
    }
    public boolean onRef(ReferenceExp exp) {
        return exp.exp.visit(this);
    }
    public boolean onSequence(SequenceExp exp) {
        return exp.exp1.visit(this) && exp.exp2.visit(this);
    }

    // unsupported
    public boolean onConcur(ConcurExp exp) {
        throw new InternalError("<concur> is not supported");
    }
}
