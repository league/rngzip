/*
 * @(#)$Id: Driver.java,v 1.47 2002/03/21 14:21:25 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.driver.textui;

import javax.xml.parsers.*;
import com.sun.msv.grammar.xmlschema.*;
import com.sun.msv.grammar.trex.*;
import com.sun.msv.grammar.relax.*;
import com.sun.msv.grammar.*;
import com.sun.msv.grammar.util.ExpressionPrinter;
import com.sun.msv.reader.util.GrammarLoader;
import com.sun.msv.relaxns.grammar.RELAXGrammar;
import com.sun.msv.relaxns.verifier.SchemaProviderImpl;
import com.sun.msv.verifier.*;
import com.sun.msv.verifier.identity.IDConstraintChecker;
import com.sun.msv.verifier.regexp.REDocumentDeclaration;
import com.sun.msv.verifier.util.ErrorHandlerImpl;
import com.sun.msv.util.Util;
import org.iso_relax.dispatcher.Dispatcher;
import org.iso_relax.dispatcher.SchemaProvider;
import org.iso_relax.dispatcher.impl.DispatcherImpl;
import org.xml.sax.*;
import java.util.*;

/**
 * command line Verifier.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class Driver {
	
	static SAXParserFactory factory;
	
    /** Prints the usage screen. */
    private static void usage() {
		System.out.println( localize(MSG_USAGE) );
    }
    
    /** Prints the version number. */
    private static void printVersion() {
	    System.out.println("Multi Schema Validator Ver."+
	    	java.util.ResourceBundle.getBundle("version").getString("version") );
    }
    
	public static void main( String[] args ) throws Exception {
        System.exit(run(args));
    }
    
    public static int run( String[] args ) throws Exception {
		final Vector fileNames = new Vector();
		
		String grammarName = null;
		boolean dump=false;
		boolean verbose = false;
		boolean warning = false;
		boolean standalone=false;
		boolean strict=false;
        boolean usePanicMode=true;
		EntityResolver entityResolver=null;
		
		for( int i=0; i<args.length; i++ ) {
            if( args[i].equalsIgnoreCase("-h")
            ||  args[i].equalsIgnoreCase("-help")
            ||  args[i].equalsIgnoreCase("-?")) {
                usage();
                return -1;
            }
			if( args[i].equalsIgnoreCase("-strict") )			strict = true;
			else
			if( args[i].equalsIgnoreCase("-standalone") )		standalone = true;
			else
			if( args[i].equalsIgnoreCase("-loose") )			standalone = true;	// backward compatible name
			else
			if( args[i].equalsIgnoreCase("-dtd") )
				; // this option is ignored.
			else
			if( args[i].equalsIgnoreCase("-dump") )				dump = true;
			else
			if( args[i].equalsIgnoreCase("-debug") )			Debug.debug = true;
			else
			if( args[i].equalsIgnoreCase("-xerces") )
				factory = (SAXParserFactory)Class.forName("org.apache.xerces.jaxp.SAXParserFactoryImpl").newInstance();
			else
			if( args[i].equalsIgnoreCase("-crimson") )
				factory = (SAXParserFactory)Class.forName("org.apache.crimson.jaxp.SAXParserFactoryImpl").newInstance();
			else
			if( args[i].equalsIgnoreCase("-oraclev2") )
				factory = (SAXParserFactory)Class.forName("oracle.xml.jaxp.JXSAXParserFactory").newInstance();
			else
			if( args[i].equalsIgnoreCase("-verbose") )			verbose = true;
			else
			if( args[i].equalsIgnoreCase("-warning") )			warning = true;
			else
            if( args[i].equalsIgnoreCase("-maxerror") )         usePanicMode = false;
            else
			if( args[i].equalsIgnoreCase("-locale") ) {
				String code = args[++i];
				
				int idx = code.indexOf('-');
				if(idx<0)	idx = code.indexOf('_');
				
				if(idx<0)
					Locale.setDefault( new Locale(code,"") );
				else
					Locale.setDefault( new Locale(
						code.substring(0,idx), code.substring(idx+1) ));
			}
			else
			if( args[i].equalsIgnoreCase("-catalog") ) {
				// use Sun's "XML Entity and URI Resolvers" by Norman Walsh
				// to resolve external entities.
				// http://www.sun.com/xml/developers/resolver/
                          // removing dependence on com.sun.resolver.tools.CatalogResolver
                          throw new UnsupportedOperationException("catalog");
			}
			else
			if( args[i].equalsIgnoreCase("-version") ) {
                printVersion();
				return 0;
			} else {
				if( args[i].charAt(0)=='-' ) {
					System.err.println(localize(MSG_UNRECOGNIZED_OPTION,args[i]));
                    usage();
					return -1;
				}
				
				if( grammarName==null )	grammarName = args[i];
				else {
					fileNames.add(args[i]);
				}
			}
		}
		
		if( grammarName==null ) {
			System.out.println( localize(MSG_USAGE) );
			return -1;
		}
		
        if( verbose )
            printVersion();

		
		if( factory==null )
			factory = SAXParserFactory.newInstance();
		
		if( verbose )
			System.out.println( localize( MSG_PARSER, factory.getClass().getName()) );
		
		factory.setNamespaceAware(true);
		factory.setValidating(false);
		if( !standalone && verbose )
			System.out.println( localize( MSG_DTDVALIDATION ) );
		
		if( standalone )
			try {
				factory.setFeature("http://xml.org/sax/features/validation",false);
				factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			} catch(Exception e) {
//				e.printStackTrace();
				System.out.println( localize( MSG_FAILED_TO_IGNORE_EXTERNAL_DTD ) );
			}
		else
			try {
				factory.setFeature("http://apache.org/xml/features/validation/dynamic",true);
				// turn off XML Schema validation if Xerces is used
				factory.setFeature("http://apache.org/xml/features/validation/schema",false);
			} catch( Exception e ) {
				;
			}
		
		

	// parse schema
	//--------------------
		final long stime = System.currentTimeMillis();
		System.out.println( localize(MSG_START_PARSING_GRAMMAR) );

		Grammar grammar=null;
		try {
			GrammarLoader loader = new GrammarLoader();
			
			// set various parameters
			loader.setController( new DebugController(warning,false,entityResolver) );
			loader.setSAXParserFactory(factory);
			loader.setStrictCheck(strict);
			
			grammar = loader.parse(grammarName);
			
		} catch(SAXParseException spe) {
			if(Debug.debug)
				spe.getException().printStackTrace();
			; // this error is already reported.
		} catch(SAXException se ) {
			if( se.getException()!=null ) throw se.getException();
			throw se;
		}
		if( grammar==null ) {
			System.out.println( localize(ERR_LOAD_GRAMMAR) );
			return -1;
		}
			
		long parsingTime = System.currentTimeMillis();
		if( verbose )
			System.out.println( localize( MSG_PARSING_TIME, new Long(parsingTime-stime) ) );

		
		if(dump) {
			if( grammar instanceof RELAXModule )
				dumpRELAXModule( (RELAXModule)grammar );
			else
			if( grammar instanceof RELAXGrammar )
				dumpRELAXGrammar( (RELAXGrammar)grammar );
			else
			if( grammar instanceof TREXGrammar )
				dumpTREX( (TREXGrammar)grammar );
			else
			if( grammar instanceof XMLSchemaGrammar )
				dumpXMLSchema( (XMLSchemaGrammar)grammar );
			
			return -1;
		}
		
	// validate documents
	//--------------------
		DocumentVerifier verifier;
		if( grammar instanceof RELAXGrammar )
			// use divide&validate framework to validate document
			verifier = new RELAXNSVerifier( new SchemaProviderImpl((RELAXGrammar)grammar) );
		else
		if( grammar instanceof XMLSchemaGrammar )
			// use verifier+identity constraint checker.
			verifier = new XMLSchemaVerifier( (XMLSchemaGrammar)grammar );
		else
			// validate normally by using Verifier.
			verifier = new SimpleVerifier( new REDocumentDeclaration(grammar) );
		
        
        boolean allValid = true;
        
		for( int i=0; i<fileNames.size(); i++ )	{
			
			final String instName = (String)fileNames.elementAt(i);
			System.out.println( localize( MSG_VALIDATING, instName) );
			
			boolean result=false;
			
			try {
				XMLReader reader = factory.newSAXParser().getXMLReader();
				if(entityResolver!=null)	reader.setEntityResolver(entityResolver);
				reader.setErrorHandler( new ReportErrorHandler() );
				
				result = verifier.verify(
					reader,
					Util.getInputSource(instName),
                    usePanicMode);
			} catch( com.sun.msv.verifier.ValidationUnrecoverableException vv ) {
				System.out.println(localize(MSG_BAILOUT));
			} catch( SAXParseException se ) {
				  se.getException().printStackTrace();
				; // error is already reported by ErrorHandler
			} catch( SAXException e ) {
				  e.getException().printStackTrace();
			}
			
			if(result)
				System.out.println(localize(MSG_VALID));
            else {
				System.out.println(localize(MSG_INVALID));
                allValid = false;
            }
            
			if( i!=fileNames.size()-1 )
				System.out.println("--------------------------------------");
		}
		
			
		if( verbose )
			System.out.println( localize( MSG_VALIDATION_TIME, new Long(System.currentTimeMillis()-parsingTime) ) );
        
        return allValid?0:-1;
	}
	
	public static void dumpTREX( TREXGrammar g ) throws Exception {
		System.out.println("*** start ***");
		System.out.println(ExpressionPrinter.printFragment(g.exp));
		System.out.println("*** others ***");
		System.out.print(
			ExpressionPrinter.fragmentInstance.printRefContainer(
				g.namedPatterns ) );
	}
	
	public static void dumpXMLSchema( XMLSchemaGrammar g ) throws Exception {
		System.out.println("*** top level ***");
		System.out.println(ExpressionPrinter.printFragment(g.topLevel));
		
		Iterator itr = g.iterateSchemas();
		while(itr.hasNext()) {
			XMLSchemaSchema s = (XMLSchemaSchema)itr.next();
			dumpXMLSchema(s);
		}
	}
	public static void dumpXMLSchema( XMLSchemaSchema s ) throws Exception {
		System.out.println("\n $$$$$$[ " + s.targetNamespace + " ]$$$$$$");

		System.out.println("*** elementDecls ***");
		ReferenceExp[] es = s.elementDecls.getAll();
		for( int i=0; i<es.length; i++ ) {
			ElementDeclExp exp = (ElementDeclExp)es[i];
			System.out.println( exp.name + "  : " +
				ExpressionPrinter.printContentModel(
                    exp.getContentModel().getExpandedExp(s.pool)) );
		}
		
		System.out.println("*** complex types ***");
		System.out.print(
			ExpressionPrinter.contentModelInstance.printRefContainer(
				s.complexTypes ) );
	}
	
	public static void dumpRELAXModule( RELAXModule m ) throws Exception {
		
		System.out.println("*** top level ***");
		System.out.println(ExpressionPrinter.printFragment(m.topLevel));
		
		System.out.println("\n $$$$$$[ " + m.targetNamespace + " ]$$$$$$");
		
		System.out.println("*** elementRule ***");
		System.out.print(
			ExpressionPrinter.fragmentInstance.printRefContainer(
				m.elementRules ) );
		System.out.println("*** hedgeRule ***");
		System.out.print(
			ExpressionPrinter.fragmentInstance.printRefContainer(
				m.hedgeRules ) );
		System.out.println("*** attPool ***");
		System.out.print(
			ExpressionPrinter.fragmentInstance.printRefContainer(
				m.attPools ) );
		System.out.println("*** tag ***");
		System.out.print(
			ExpressionPrinter.fragmentInstance.printRefContainer(
				m.tags ) );
	}

	public static void dumpRELAXGrammar( RELAXGrammar m ) throws Exception {
		System.out.println("operation is not implemented yet.");
	}

	/** acts as a function closure to validate a document. */
	private interface DocumentVerifier {
		boolean verify( XMLReader p, InputSource instance, boolean usePanicMode ) throws Exception;
	}
	
	/** validates a document by using divide &amp; validate framework. */
	private static class RELAXNSVerifier implements DocumentVerifier {
		private final SchemaProvider sp;
		
		RELAXNSVerifier( SchemaProvider sp ) { this.sp=sp; }
		
		public boolean verify( XMLReader p, InputSource instance, boolean panicMode ) throws Exception {
			Dispatcher dispatcher = new DispatcherImpl(sp);
			dispatcher.attachXMLReader(p);
			ReportErrorHandler errorHandler = new ReportErrorHandler();
			dispatcher.setErrorHandler( errorHandler );
			
            // TODO: support the panicMode argument
			p.parse(instance);
			return !errorHandler.hadError;
		}
	}
	
	private static class SimpleVerifier implements DocumentVerifier {
		private final DocumentDeclaration docDecl;
		
		SimpleVerifier( DocumentDeclaration docDecl ) { this.docDecl = docDecl; }

		public boolean verify( XMLReader p, InputSource instance, boolean panicMode ) throws Exception {
			ReportErrorHandler reh = new ReportErrorHandler();
			Verifier v = new Verifier( docDecl, reh );
            v.setPanicMode(panicMode);
		
			p.setDTDHandler(v);
			p.setContentHandler(v);
			p.setErrorHandler(reh);
		
			p.parse( instance );
			return v.isValid();
		}
	}

	private static class XMLSchemaVerifier implements DocumentVerifier {
		private final XMLSchemaGrammar grammar;
		
		XMLSchemaVerifier( XMLSchemaGrammar grammar ) { this.grammar = grammar; }

		public boolean verify( XMLReader p, InputSource instance, boolean panicMode ) throws Exception {
			ReportErrorHandler reh = new ReportErrorHandler();
			Verifier v = new IDConstraintChecker( grammar, reh );
            v.setPanicMode(panicMode);
		
			p.setDTDHandler(v);
			p.setContentHandler(v);
			p.setErrorHandler(reh);
		
			p.parse( instance );
			return v.isValid();
		}
	}

	public static String localize( String propertyName, Object[] args ) {
		String format = java.util.ResourceBundle.getBundle(
			"com.sun.msv.driver.textui.Messages").getString(propertyName);
	    return java.text.MessageFormat.format(format, args );
	}
	public static String localize( String prop )
	{ return localize(prop,null); }
	public static String localize( String prop, Object arg1 )
	{ return localize(prop,new Object[]{arg1}); }
	public static String localize( String prop, Object arg1, Object arg2 )
	{ return localize(prop,new Object[]{arg1,arg2}); }
	
	public static final String MSG_DTDVALIDATION =		"Driver.DTDValidation";
	public static final String MSG_PARSER =				"Driver.Parser";
	public static final String MSG_USAGE =				"Driver.Usage";
	public static final String MSG_UNRECOGNIZED_OPTION ="Driver.UnrecognizedOption";
	public static final String MSG_START_PARSING_GRAMMAR="Driver.StartParsingGrammar";
	public static final String MSG_PARSING_TIME =		"Driver.ParsingTime";
	public static final String MSG_VALIDATING =			"Driver.Validating";
	public static final String MSG_VALIDATION_TIME =	"Driver.ValidationTime";
	public static final String MSG_VALID =				"Driver.Valid";
	public static final String MSG_INVALID =			"Driver.Invalid";
	public static final String ERR_LOAD_GRAMMAR =		"Driver.ErrLoadGrammar";
	public static final String MSG_BAILOUT =			"Driver.BailOut";
	public static final String MSG_FAILED_TO_IGNORE_EXTERNAL_DTD ="Driver.FailedToIgnoreExternalDTD";
	public static final String MSG_WARNING_FOUND =		"Driver.WarningFound";
}
