package org.kohsuke.bali.datatype;

import org.relaxng.datatype.Datatype;
import org.relaxng.datatype.DatatypeBuilder;
import org.relaxng.datatype.DatatypeException;
import org.relaxng.datatype.DatatypeLibrary;
import org.relaxng.datatype.helpers.DatatypeLibraryLoader;

import com.sun.msv.grammar.relaxng.datatype.BuiltinDatatypeLibrary;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class DatatypeLibraryImpl implements DatatypeLibrary {

    public DatatypeLibraryImpl( String nsURI ) {
        this.nsURI = nsURI;
        
        if(nsURI.length()==0)
            realLib = BuiltinDatatypeLibrary.theInstance;
        else
            realLib = new DatatypeLibraryLoader().createDatatypeLibrary(nsURI);
    }
    
    /** Datatype libarry URI. */
    private final String nsURI;
    
    /** Actual datatype library implementation. */
    protected final DatatypeLibrary realLib;
    
    public DatatypeBuilder createDatatypeBuilder(String dtName)
        throws DatatypeException {
            
        return new DatatypeBuilderImpl(nsURI,dtName,
            realLib.createDatatypeBuilder(dtName));    
    }

    public Datatype createDatatype(String dtName) throws DatatypeException {
        return createDatatypeBuilder(dtName).createDatatype();
    }

}
