package org.kohsuke.bali.automaton;

import java.util.Collection;

import com.sun.msv.grammar.*;
import com.sun.msv.grammar.util.NameClassSimplifier;

/**
 * A transition with a non-existent attribute alphabet can be taken
 * only when none of the current attribute matches the set of
 * names contained 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NonExistentAttributeAlphabet extends Alphabet {
    
    // attributes with these names are not allowed
    public final NameSignature[] negativeNameTests;
    // with the exception of these names
    public final NameSignature[] positiveNameTests;
    
    /**
     * Exact representation of the above semantics as one name class.
     * Used just for debugging.
     */
    private final NameClass exactName;
    
    public NonExistentAttributeAlphabet( Collection neg, Collection pos ) {
        this((NameSignature[]) neg.toArray(new NameSignature[neg.size()]),
             (NameSignature[]) pos.toArray(new NameSignature[pos.size()]));
    }
    
    public NonExistentAttributeAlphabet( NameSignature[] neg, NameSignature[] pos ) {
        this.negativeNameTests = neg;
        this.positiveNameTests = pos;
        
        NameClass n = new NotNameClass(AnyNameClass.theInstance);
        for( int i=0; i<neg.length; i++ )
            n = new ChoiceNameClass( n, neg[i].nameClass );
        for( int i=0; i<pos.length; i++ )
            n = new DifferenceNameClass( n, pos[i].nameClass );
        
        exactName = NameClassSimplifier.simplify(n);
    }
    
    /**
     * Checks if the given attribute name is prohibited by this alphabet.
     */
    public boolean accepts( int nameCode ) {
        for( int i=positiveNameTests.length-1; i>=0; i-- )
            if( positiveNameTests[i].accepts(nameCode) )
                return false;
        
        for( int i=negativeNameTests.length-1; i>=0; i-- )
            if( negativeNameTests[i].accepts(nameCode) )
                return true;    // prohibited
        
        return false;   // otherwise OK
    }
    
    
    public Object accept( AlphabetVisitor visitor ) {
        return visitor.nonExistentAttribute(this);
    }
    
    public String toString() {
        return "!@"+exactName.toString();
    }

    public boolean isPersistent() { return false; }
}
