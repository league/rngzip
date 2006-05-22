package org.kohsuke.validatelet.jarv;

import org.iso_relax.verifier.*;
import org.iso_relax.verifier.impl.VerifierImpl;
import org.kohsuke.validatelet.Validatelet;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * Wraps a {@link Validatelet} object into a JARV {@link Verifier} interface.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JARVVerifierImpl extends VerifierImpl {
    
    /**
     * @param validatelet
     *      The validatelet object to be wrapped. This validatelet
     *      will be "owned" by this JARVVerifierImpl, so the caller
     *      shouldn't attempt to use directly once it's wrapped.
     */
    public JARVVerifierImpl( Validatelet validatelet ) throws VerifierConfigurationException {
        this.validatelet = validatelet;
        this.handler = new JARVVerifierHandlerImpl(validatelet);
    }
    
    
    private final Validatelet validatelet;
    
    private final VerifierHandler handler;

    public VerifierHandler getVerifierHandler() throws SAXException {
        return handler;
    }

    public void setErrorHandler(ErrorHandler handler) {
        super.setErrorHandler(handler);
        validatelet.setErrorHandler(handler);
    }

}
