package org.kohsuke.bali.datatype;

/**
 * Parameter added to a datatype.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Parameter {
    public Parameter( String name, String value, ValidationContextImpl context ) {
        this.name = name;
        this.value = value;
        this.context = context;
    }
    
    /** Parameter name. */
    public final String name;
    /** Parameter value. */
    public final String value;
    
    /** Context under which the value should be evaluated. */
    public final ValidationContextImpl context;
}
