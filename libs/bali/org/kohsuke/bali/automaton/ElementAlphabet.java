package org.kohsuke.bali.automaton;

import com.sun.msv.grammar.NameClass;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ElementAlphabet extends Alphabet {
    
    /** Name test of this element declaration. */
    public final NameSignature name;
    
    public ElementAlphabet( NameSignature name ) {
        this.name = name;
    }
    
    public Object accept( AlphabetVisitor visitor ) {
        return visitor.element(this);
    }
    
    public boolean isPersistent() { return true; }
    
    public String toString() {
        return "<"+name.nameClass.toString()+">";
    }
}
