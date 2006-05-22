package org.kohsuke.bali.automaton;

import com.sun.msv.grammar.NameClass;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AttributeAlphabet extends Alphabet {
    
    /** Name test of this element declaration. */
    public final NameSignature name;
    
    /**
     * True if this alphabet allows multiple attributes that
     * matches the name class. If false, then having two attributes
     * that matches to the name class is an error.
     */
    public final boolean repeated;
    
    
    public AttributeAlphabet( NameSignature name, boolean rep ) {
        this.name = name;
        this.repeated = rep;
    }
    
    public Object accept( AlphabetVisitor visitor ) {
        return visitor.attribute(this);
    }

    public boolean isPersistent() { return false; }
    
    public String toString() {
        return "@"+name.nameClass.toString();
    }
}
