package org.kohsuke.bali.optimizer;

import java.util.HashSet;
import java.util.Set;

import com.sun.msv.grammar.AttributeExp;
import com.sun.msv.grammar.ChoiceExp;
import com.sun.msv.grammar.ElementExp;
import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.ExpressionCloner;
import com.sun.msv.grammar.ExpressionPool;
import com.sun.msv.grammar.Grammar;
import com.sun.msv.grammar.OneOrMoreExp;
import com.sun.msv.grammar.OtherExp;
import com.sun.msv.grammar.ReferenceExp;
import com.sun.msv.grammar.SimpleNameClass;
import com.sun.msv.grammar.trex.TREXGrammar;

/**
 * Looks for
 * <xmp>
 * <zeroOrMore>
 *   <choice>
 *     <attribute name="foo">
 *       ...
 *     </attribute>
 *     <attribute name="bar">
 *       ...
 *     </attribute>
 *   </choice>
 * </zeroOrMore>
 * </xmp>
 * 
 * and replace it with
 * 
 * <xmp>
 * <optional>
 *   <attribute name="foo">
 *     ...
 *   </attribute>
 * </optional>
 * <optional>
 *   <attribute name="bar">
 *     ...
 *   </attribute>
 * </optional>
 * </xmp>
 * 
 * which is simpler.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ZeroOrMoreAttributeExpander extends ExpressionCloner {
    
    private final Set visitedElementExps = new HashSet();

    private ZeroOrMoreAttributeExpander(ExpressionPool pool) {
        super(pool);
    }
    
    public static Grammar optimize( Grammar grammar ) {
        ExpressionPool pool = grammar.getPool();
        TREXGrammar result = new TREXGrammar(pool);
        ZeroOrMoreAttributeExpander r = new ZeroOrMoreAttributeExpander(pool);
        result.exp = grammar.getTopLevel().visit(r);
        return result;
    }
    

    public Expression onChoice(ChoiceExp exp) {
        if( exp.isEpsilonReducible() ) {
            // look for OneOrMoreExp
            Expression[] children = exp.getChildren();
            for( int i=0; i<children.length; i++ ) {
                if( children[i] instanceof OneOrMoreExp ) {
//                    System.err.println("zom: "+ExpressionPrinter.printContentModel(children[i]));
                    
                    OneOrMoreExp oom = (OneOrMoreExp)children[i];
                    Expression child = oom.exp;
                    if( child instanceof AttributeExp ) {
                        children[i] = wrapAttribute( (AttributeExp)child );
                    } else
                    if( child instanceof ChoiceExp ) {
                        Expression r = Expression.nullSet;  // untouched items
                        children[i] = Expression.epsilon;
                        
                        Expression[] grandSons = ((ChoiceExp)child).getChildren();
                        for( int j=0; j<grandSons.length; j++ ) {
                            if( grandSons[j] instanceof AttributeExp )
                                children[i] = pool.createSequence(children[i],
                                    wrapAttribute( (AttributeExp)grandSons[j] ));
                            else
                                r = pool.createChoice(r,grandSons[j]);
                        }
                        
                        children[i] = pool.createSequence(children[i],
                            pool.createZeroOrMore(r));
                    } else {
                        // otherwise we can't do anything about this.
                        // leave this branch untouched
                        children[i] = children[i].visit(this);
                    }
                } else {
                    children[i] = children[i].visit(this);
                }
            }
            
            Expression r = Expression.nullSet;
            for( int i=0; i<children.length; i++ )
                r = pool.createChoice(r,children[i]);
            return r;
        }
        
        // by default
        return super.onChoice(exp);
    }
    
    private Expression wrapAttribute( AttributeExp exp ) {
        if( exp.nameClass instanceof SimpleNameClass )
            return pool.createOptional(exp);
        else
            return pool.createZeroOrMore(exp);
    }

    public Expression onAttribute(AttributeExp exp) {
        return exp;
    }

    public Expression onElement(ElementExp exp) {
        if( visitedElementExps.add(exp) )
            exp.contentModel = exp.contentModel.visit(this);
        return exp;
    }

    public Expression onOther(OtherExp exp) {
        return exp.exp.visit(this);
    }

    public Expression onRef(ReferenceExp exp) {
        return exp.exp.visit(this);
    }

    public Expression onOneOrMore(OneOrMoreExp exp) {
        return super.onOneOrMore(exp);
    }

}
