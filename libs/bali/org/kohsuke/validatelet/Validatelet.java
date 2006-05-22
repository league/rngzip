package org.kohsuke.validatelet;

import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Validator implemented as a SAX {@link ContentHandler}.
 * 
 * By calling SAX callback methods, this object performs
 * a validation.
 * 
 * Detected errors will be sent to the specified error handler.
 * If no error handler is specified, it throws a {@link SAXParseException}.
 * 
 * <p>
 * Calling the startDocument method will reset a validatelet
 * and make it ready to accept a new document.
 * Thus a client can re-use the same instance of Validatelet
 * many times.
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface Validatelet extends ContentHandler {
    /**
     * Sets the error handler, which will receive validation errors
     * detected during the validation.
     */
    void setErrorHandler( ErrorHandler errorHandler );
    
    /**
     * Gets the error handler passed by the setErrorHandler method,
     * or null if none was specified.
     */
    ErrorHandler getErrorHandler();
}
