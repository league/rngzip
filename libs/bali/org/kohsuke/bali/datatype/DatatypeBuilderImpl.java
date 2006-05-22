package org.kohsuke.bali.datatype;

import java.util.ArrayList;
import java.util.List;

import org.relaxng.datatype.Datatype;
import org.relaxng.datatype.DatatypeBuilder;
import org.relaxng.datatype.DatatypeException;
import org.relaxng.datatype.DatatypeLibrary;
import org.relaxng.datatype.ValidationContext;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class DatatypeBuilderImpl implements DatatypeBuilder {
    DatatypeBuilderImpl( String nsURI, String dtName, DatatypeBuilder realBuilder ) {
        this.nsURI = nsURI;
        this.dtName = dtName;
        this.realBuilder = realBuilder;
    }    
    
    private final DatatypeBuilder realBuilder;
    
    private final String nsURI,dtName;

    public void addParameter(String name, String value, ValidationContext context)
        throws DatatypeException {
        
        ValidationContextImpl vcimpl = new ValidationContextImpl(context);
        realBuilder.addParameter(name,value,vcimpl);
        
        parameters.add( new Parameter(name,value,vcimpl) );
    }
    
    private List parameters = new ArrayList();

    public Datatype createDatatype() throws DatatypeException {
        return new DatatypeImpl( nsURI, dtName, new ArrayList(parameters), realBuilder.createDatatype() );
    }

}
