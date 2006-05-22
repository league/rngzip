package org.kohsuke.bali.datatype;

import org.relaxng.datatype.DatatypeLibrary;
import org.relaxng.datatype.DatatypeLibraryFactory;

/**
 * DatatypeLibraryFactory implementation that just
 * preserves all the parameters AS-IS, so that we can re-construct
 * the exact paramters in the output.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class DatatypeLibraryFactoryImpl implements DatatypeLibraryFactory {

    public DatatypeLibrary createDatatypeLibrary(String nsURI) {
        return new DatatypeLibraryImpl(nsURI);
    }

}
