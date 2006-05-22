package org.kohsuke.bali.automaton.builder;

import com.sun.msv.grammar.*;
import com.sun.msv.grammar.util.NameClassSimplifier;

/**
 * Find all AttributeExps inside the given expression
 * and combine them into a single NameClass.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class AttNameCombiner implements ExpressionVisitor {

    /**
     * Obtains the all permitted attributes as a name class.
     */
    public static NameClass collect( Expression exp ) {
        return NameClassSimplifier.simplify((NameClass)exp.visit(theInstance));
    }
    
    private static final AttNameCombiner theInstance = new AttNameCombiner();


    /**
     * Empty name class.
     * 
     * Used very frequently so this field is used as a constant.
     */
    private final NameClass noName = new NotNameClass(AnyNameClass.theInstance);
    
    public Object onAttribute(AttributeExp att) {
        return att.nameClass;
    }

    public Object onChoice(ChoiceExp exp)         { return binary(exp); }
    public Object onInterleave(InterleaveExp exp) { return binary(exp); }
    public Object onSequence(SequenceExp exp)     { return binary(exp); }
    
    private NameClass binary( BinaryExp exp ) {
        NameClass nc1 = (NameClass)exp.exp1.visit(this);
        NameClass nc2 = (NameClass)exp.exp2.visit(this);
        if( nc1==noName )   return nc2;
        if( nc2==noName )   return nc1;
        return new ChoiceNameClass(nc1,nc2);
    }

    public Object onConcur(ConcurExp exp) {
        throw new InternalError("<concur> is not supported");
    }
    
    public Object onData(DataExp exp)       { return noName; }
    public Object onElement(ElementExp exp) { return noName; }
    public Object onEpsilon()               { return noName; }
    public Object onList(ListExp exp)       { return noName; }
    public Object onMixed(MixedExp exp)     { return exp.exp.visit(this); }
    public Object onNullSet()               { throw new InternalError(); }
    public Object onOneOrMore(OneOrMoreExp exp) { return exp.exp.visit(this); }
    public Object onOther(OtherExp exp)     { return exp.exp.visit(this); }
    public Object onRef(ReferenceExp exp)   { return exp.exp.visit(this); }
    public Object onValue(ValueExp exp)     { return noName; }
    public Object onAnyString()             { return noName; }
};
