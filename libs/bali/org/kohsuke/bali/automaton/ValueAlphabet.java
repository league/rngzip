package org.kohsuke.bali.automaton;

import org.relaxng.datatype.Datatype;

/**
 * Alphabet by text.
 * 
 * A transition with a DataAlpahbet will not have its left state.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ValueAlphabet extends TextAlphabet {
    /** Datatype object. */
    public final Datatype datatype;
    /** Value to be tested against. */
    public final Object value;
    
    public ValueAlphabet( Datatype dt, Object value ) {
        this.datatype = dt;
        this.value = value;
    }
    
    public Object accept( AlphabetVisitor visitor ) {
        return visitor.value(this);
    }

    public boolean isPersistent() { return true; }
    
    public String toString() {
        return '"'+value.toString()+'"';
    }
}
