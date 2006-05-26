package com.sun.msv.reader;

import org.xml.sax.InputSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Internal view of GrammarReaderController.
 * 
 * This class wraps a GrammarReaderController and
 * adds several convenient methods for the caller.
 */
public class Controller implements GrammarReaderController,ErrorHandler
{
    /** Controller works as a wrapper to this object. */
    private final GrammarReaderController core;
    public GrammarReaderController getCore() { return core; }
    
    /** This flag will be set to true in case of any error. */
    private boolean _hadError = false;
    
    /** Returns true if an error had been reported. */
    public boolean hadError() { return _hadError; }
    
    /** Force set the error flag to true. */
    public final void setErrorFlag() { _hadError=true; }
        
    public Controller( GrammarReaderController _core ) {
        this.core = _core;
    }
    
    public InputSource resolveEntity( String p, String s ) throws SAXException, IOException {
        return core.resolveEntity(p,s);
    }
    
    public void warning( Locator[] locs, String errorMessage ) {
        core.warning(locs,errorMessage);
    }
    
    public void error( Locator[] locs, String errorMessage, Exception nestedException ) {
        setErrorFlag();
        core.error(locs,errorMessage,nestedException);
    }
    
	public void fatalError( SAXParseException spe ) {
		error(spe);
	}
	
	public void error( SAXParseException spe ) {
		error( getLocator(spe), spe.getMessage(), spe.getException() );
	}
	
	public void warning( SAXParseException spe ) {
		warning( getLocator(spe), spe.getMessage() );
	}
	
    public void error( IOException e, Locator source ) {
        error( new Locator[]{source}, e.getMessage(), e );
    }
	
    public void error( SAXException e, Locator source ) {
        // if a nested exception is a RuntimeException,
        // this shouldn't be handled.
        if( e.getException() instanceof RuntimeException )
            throw (RuntimeException)e.getException();
        
        if(e instanceof SAXParseException)
            error( (SAXParseException)e );
        else
            error( new Locator[]{source}, e.getMessage(), e );
    }
    
    public void error( ParserConfigurationException e, Locator source ) {
        error( new Locator[]{source}, e.getMessage(), e );
    }
    
    
    
	protected Locator[] getLocator( SAXParseException spe ) {
		LocatorImpl loc = new LocatorImpl();
		loc.setColumnNumber( spe.getColumnNumber() );
		loc.setLineNumber( spe.getLineNumber() );
		loc.setSystemId( spe.getSystemId() );
		loc.setPublicId( spe.getPublicId() );
		
		return new Locator[]{loc};
	}
}
