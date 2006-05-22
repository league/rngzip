package org.kohsuke.validatelet.jarv;

import org.iso_relax.verifier.VerifierHandler;
import org.kohsuke.validatelet.Validatelet;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * {@link VerifierHandler} implementation that wraps a
 * Validatelet interface.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class JARVVerifierHandlerImpl extends XMLFilterImpl implements VerifierHandler {
    
    JARVVerifierHandlerImpl( Validatelet validatelet ) {
        super.setContentHandler(validatelet);
    }
    

    public boolean isValid() throws IllegalStateException {
        if(isValidating)    throw new IllegalStateException();
        // a validatelet throws a SAXParseException whenever a problem is
        // encountered. Thus if the validation completes without an exception
        // thrown, then that means the document was valid.
        return true;
    }

    public void startDocument() throws SAXException {
        super.startDocument();
        isValidating = true;
    }
    public void endDocument() throws SAXException {
        super.endDocument();
        isValidating = false;
    }
    
    private boolean isValidating = true;


}
