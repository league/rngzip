package org.kohsuke.bali.automaton.builder;

import java.util.HashMap;
import java.util.Map;
import javax.naming.NameClassPair;

import com.sun.msv.grammar.AnyNameClass;
import com.sun.msv.grammar.ChoiceNameClass;
import com.sun.msv.grammar.DifferenceNameClass;
import com.sun.msv.grammar.NameClass;
import com.sun.msv.grammar.NameClassVisitor;
import com.sun.msv.grammar.NamespaceNameClass;
import com.sun.msv.grammar.NotNameClass;
import com.sun.msv.grammar.SimpleNameClass;
import com.sun.msv.grammar.util.NameClassSimplifier;
import com.sun.msv.util.StringPair;

/**
 * Unifies name class objects so that two equivalent name classes
 * can be compared by referencial identitiy.
 * 
 * The caller should use the <code>unify</code> method.
 * Firstly, structual equivalence is guaranteed by the NameClassSimplifier,
 * and then referencial equivalence is guaranteed by interning.
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NameClassUnifier implements NameClassVisitor {
    
    public NameClass unify( NameClass nc ) {
        return (NameClass)NameClassSimplifier.simplify(nc).visit(this);
    } 

    /** Pair of NameClasses. */
    private static class NameClassPair {
        public final NameClass lhs,rhs;
        public NameClassPair( NameClass l, NameClass r ) {
            this.lhs=l; this.rhs=r;
        }
        
        public int hashCode() { return lhs.hashCode()^rhs.hashCode(); }
        public boolean equals( Object o ) {
            return ((NameClassPair)o).rhs==rhs && ((NameClassPair)o).lhs==lhs;
        }
    }

    private Map choices = new HashMap();
    public Object onChoice(ChoiceNameClass nc) {
        NameClassPair p = new NameClassPair( (NameClass)nc.nc1.visit(this), (NameClass)nc.nc2.visit(this) );
        
        nc = (ChoiceNameClass)choices.get(p);
        if(nc==null)
            choices.put(p,nc=new ChoiceNameClass(p.lhs,p.rhs));
        return nc;
    }

    public Object onAnyName(AnyNameClass nc) {
        return AnyNameClass.theInstance;
    }

    private Map simples = new HashMap();
    public Object onSimple(SimpleNameClass nc) {
        StringPair p = new StringPair( nc.namespaceURI.intern(), nc.localName.intern() );
        
        nc = (SimpleNameClass)simples.get(p);
        if(nc==null)
            simples.put(p,nc=new SimpleNameClass(p.namespaceURI,p.localName));
        return nc;
    }

    private Map namespaces = new HashMap();
    public Object onNsName(NamespaceNameClass nc) {
        String ns = nc.namespaceURI.intern();
        
        nc = (NamespaceNameClass)namespaces.get(ns);
        if(nc==null)
            namespaces.put(ns,nc=new NamespaceNameClass(ns));
        return nc;
    }

    private Map nots = new HashMap();
    public Object onNot(NotNameClass nc) {
        NameClass n = (NameClass)nc.child.visit(this);
        
        nc = (NotNameClass)nots.get(n);
        if(nc==null)
            nots.put(n,nc=new NotNameClass(n));
        return nc;
    }

    private Map differences = new HashMap();
    public Object onDifference(DifferenceNameClass nc) {
        NameClassPair p = new NameClassPair( (NameClass)nc.nc1.visit(this), (NameClass)nc.nc2.visit(this) );
        
        nc = (DifferenceNameClass)differences.get(p);
        if(nc==null)
            differences.put(p,nc=new DifferenceNameClass(p.lhs,p.rhs));
        return nc;
    }
}
