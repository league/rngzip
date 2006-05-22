package org.kohsuke.bali.datatype;

/**
 * Value created from a datatype.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Value {
    Value( DatatypeImpl _parent, String _value, ValidationContextImpl _context ) {
        this.parent = _parent;
        this.value = _value;
        this.context= _context;
    }
    
    /** Datatype object from which this value was created. */
    public final DatatypeImpl parent;
    
    /** Lexical representation of the value. */
    public final String value;
    
    /** Context under which the value should be evaluated. */
    public final ValidationContextImpl context;
    
    public String toString() { return '"'+value.trim()+'"'; }
}
