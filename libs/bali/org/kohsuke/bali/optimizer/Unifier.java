package org.kohsuke.bali.optimizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.kohsuke.bali.automaton.builder.NameClassUnifier;

import com.sun.msv.grammar.AnyNameClass;
import com.sun.msv.grammar.AttributeExp;
import com.sun.msv.grammar.DifferenceNameClass;
import com.sun.msv.grammar.ElementExp;
import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.ExpressionCloner;
import com.sun.msv.grammar.ExpressionPool;
import com.sun.msv.grammar.Grammar;
import com.sun.msv.grammar.NameClass;
import com.sun.msv.grammar.NameClassAndExpression;
import com.sun.msv.grammar.NotNameClass;
import com.sun.msv.grammar.OtherExp;
import com.sun.msv.grammar.ReferenceExp;
import com.sun.msv.grammar.SimpleNameClass;
import com.sun.msv.grammar.trex.ElementPattern;
import com.sun.msv.grammar.trex.TREXGrammar;
import com.sun.msv.grammar.util.NameClassSimplifier;

/**
 * Agressively unifies AttributeExps and ElementExps.
 * 
 * This will make the grammar size smaller, which in turn
 * make the size of automaton smaller.
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Unifier extends ExpressionCloner {
    
    // use the unify method.
    private Unifier( ExpressionPool pool ) { super(pool); }
    
    /**
     * Reduces the size of the grammar by unification of
     * redundant attribute/element declarations.
     */
    public static Grammar unify( Grammar g ) {
        ExpressionPool newPool = new ExpressionPool();
        TREXGrammar result = new TREXGrammar(newPool);
        Unifier u = new Unifier(newPool);
        result.exp = g.getTopLevel().visit(u);
        return result;
    }

    /**
     * Attribute/Element declaration, which consists of 
     * the name of it and the content model of it.
     * 
     * Equality is implemented in such a way that
     * <code>lhs.equals(rhs)</code> iff two same declarations are the same.
     */
    private static class Decl {
        final Expression contentModel;
        final NameClass  name;
        
        Decl( NameClass name, Expression contentModel ) {
            this.name = name;
            this.contentModel = contentModel;
        }
        
        // nameClass doesn't implement hashCode correctly.
        public int hashCode() { return contentModel.hashCode(); }
        
        public boolean equals(Object o) {
            if(!(o instanceof Decl))    return false;
            return equals(name,((Decl)o).name) && contentModel==((Decl)o).contentModel;
        }
        
        private static boolean equals( NameClass nc1, NameClass nc2 ) {
            if(isNull(NameClassSimplifier.simplify( new DifferenceNameClass(nc1,nc2) ))
            && isNull(NameClassSimplifier.simplify( new DifferenceNameClass(nc2,nc1) )))
                return true;
            else
                return false;
        }
        
        /**
         * Returns true if the name class accepts nothing.
         */
        private static boolean isNull( NameClass nc ) {
            return nc instanceof NotNameClass
                && ((NotNameClass)nc).child instanceof AnyNameClass;
        }
    }
    
    /** A map from Decl to ElementExp. */
    private final Map elementDecls = new HashMap();
    
    /** A map from Decl to AttributeExp. */
    private final Map attributeDecls = new HashMap();
    
    /** NameClass unifier. */
    private final NameClassUnifier ncUnifier = new NameClassUnifier();

    public Expression onAttribute(AttributeExp exp) {
        NameClass name = ncUnifier.unify(exp.nameClass);
        
        // unifies this expression
        Decl d = new Decl(name,exp.exp.visit(this));
        AttributeExp r = (AttributeExp)attributeDecls.get(d);
        if(r==null) {
//            System.out.println("@"+d.name);
            attributeDecls.put(d,r = (AttributeExp)pool.createAttribute(d.name,d.contentModel));
        }
        return r;
    }

    public Expression onElement(ElementExp exp) {
        NameClass name = ncUnifier.unify(exp.getNameClass());
    
        // unifies this expression
        Decl d = new Decl(name,exp.contentModel.visit(this));
        ElementExp r = (ElementExp)elementDecls.get(d);
        if(r==null)
            elementDecls.put(d,r = new ElementPattern(d.name,d.contentModel));
        return r;
    }

    public Expression onOther(OtherExp exp) {
        // remove OtherExps
        return exp.exp.visit(this);
    }
    

    
    /**
     * A set used to remember all processed ReferenceExps.
     * Necessary to stop infinite recursion.
     */
    private final Map refExps = new HashMap();
    
    public Expression onRef(ReferenceExp exp) {
        ReferenceExp n = (ReferenceExp)refExps.get(exp);
        if(n!=null)     return n;
        
        refExps.put(exp, n = new ReferenceExp(null));
        n.exp = exp.exp.visit(this);
        
        return n;
    }

}
