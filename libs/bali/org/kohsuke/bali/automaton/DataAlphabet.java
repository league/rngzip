package org.kohsuke.bali.automaton;

import org.kohsuke.bali.datatype.DatatypeImpl;
import org.relaxng.datatype.Datatype;

import com.sun.msv.datatype.xsd.StringType;

/**
 * Alphabet by text.
 * 
 * A transition with a DataAlpahbet will not have its left state.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class DataAlphabet extends TextAlphabet {
    /** Datatype object. */
    public final Datatype datatype;
    
    public DataAlphabet( Datatype dt ) {
        this.datatype = dt;
    }
    
    public Object accept( AlphabetVisitor visitor ) {
        return visitor.data(this);
    }
    
    public boolean isPersistent() { return true; }
    
    public boolean isAlwaysValid() {
        return ((DatatypeImpl)datatype).realDatatype==StringType.theInstance;
    }
}
