package org.kohsuke.bali.automaton;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class Transition {
    
    /** Alphabet of this transition. */
    public final Alphabet alphabet;
    
    /** State for the first child. */
    public final State  left;
    
    /** State for the next sibling. */
    public final State  right;
    
    
    public Transition( Alphabet a, State l, State r ) {
        this.alphabet = a;
        this.left = l;
        this.right = r;
        
        // sanity check
        if( (a instanceof ElementAlphabet) || (a instanceof AttributeAlphabet) )
            if(l==null)
                throw new InternalError();
    }

}
