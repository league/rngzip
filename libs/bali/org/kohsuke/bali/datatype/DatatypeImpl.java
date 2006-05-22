package org.kohsuke.bali.datatype;

import java.util.ArrayList;
import java.util.List;

import org.relaxng.datatype.Datatype;
import org.relaxng.datatype.DatatypeBuilder;
import org.relaxng.datatype.DatatypeException;
import org.relaxng.datatype.DatatypeLibrary;
import org.relaxng.datatype.DatatypeStreamingValidator;
import org.relaxng.datatype.ValidationContext;
import org.relaxng.datatype.helpers.DatatypeLibraryLoader;
import org.relaxng.datatype.helpers.StreamingValidatorImpl;

import com.sun.msv.grammar.relaxng.datatype.BuiltinDatatypeLibrary;

/**
 * Datatype object that keeps track of all parameters
 * so that we can later retrieve all the information
 * necessary to re-build this datatype.
 * 
 * <p>
 * This object implements the equals method so that two
 * equivalent datatype objects will be unified into one.
 */
public class DatatypeImpl implements Datatype {

    public DatatypeImpl( String nsURI, String name, Datatype realDatatype ) {
        this( nsURI, name, new ArrayList(), realDatatype );
    }
    
    public DatatypeImpl( String nsURI, String name, List parameters, Datatype realDatatype ) {
        this.nsURI = nsURI;
        this.name = name;
        this.parameters = (Parameter[])parameters.toArray(new Parameter[parameters.size()]);
        this.realDatatype = realDatatype;
    }
    
    /** datatype name.*/
    public final String nsURI;
    public final String name;

    /** applied parameters. */
    public final Parameter[] parameters;
    
    /** real datatype object. */
    public final Datatype realDatatype;

    public boolean isValid(String text, ValidationContext context) {
        return true;
    }

    public void checkValid(String text, ValidationContext context) throws DatatypeException {
    }

    public DatatypeStreamingValidator createStreamingValidator(ValidationContext context) {
        return new StreamingValidatorImpl(this,context);
    }

    /**
     * Creates a {@link Value} object.
     */
    public Object createValue(String value, ValidationContext vc) {
        // feed this to the real datatype object and see if it likes it.
        // this step also corrects context information necessary to re-parse
        // this value.
        ValidationContextImpl context = new ValidationContextImpl(vc);
        if( realDatatype.createValue(value,context)==null )
            return null;    // this is not an OK value for this datatype
            
        // return a value object
        return new Value( this, value, context );
    }

    public boolean sameValue(Object v1, Object v2) {
        return v1.equals(v2);
    }

    public int valueHashCode(Object v) {
        return v.hashCode();
    }

    public int getIdType() { return ID_TYPE_NULL; }
    
    public boolean isContextDependent() { return false; }
    
    
    
    public int hashCode() {
        return name.hashCode() ^ nsURI.hashCode() ^ parameters.hashCode();
    }
    public boolean equals( Object o ) {
        if(!(o instanceof DatatypeImpl))    return false;
        
        // this object is not context safe. avoid merging.
        if( realDatatype.isContextDependent() && parameters.length!=0 )
            return false;
        
        DatatypeImpl rhs = (DatatypeImpl)o;
        
        if( this.nsURI.equals(rhs.nsURI)
        &&  this.name.equals(rhs.name)
        &&  this.parameters.equals(rhs.parameters) )
            return true;
        
        return false;
    }
}
