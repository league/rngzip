/*
 * @(#)$Id: LazyTypeIncubator.java,v 1.3 2002/10/06 18:07:05 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.datatype.xsd;

/*
import com.sun.msv.datatype.xsd.StringType;
 */
import com.sun.msv.datatype.xsd.TypeIncubator;
import com.sun.msv.datatype.xsd.XSDatatype;
import com.sun.msv.reader.GrammarReader;
import org.relaxng.datatype.ValidationContext;
import org.relaxng.datatype.DatatypeException;
import java.util.List;
import java.util.Iterator;

/**
 * Lazy XSTypeIncubator
 * 
 * <p>
 * This incubator is used to add facets to lazily created XSDatatypeExp object.
 * Since the actual Datatype object is not available when facets are parsed,
 * this object merely stores all facets when the addFacet method is called.
 * 
 * <p>
 * Once the actual Datatype is provided, this class uses ordinary
 * TypeIncubator and builds a real type object.
 *
 * @author <a href="mailto:kohsuke.kawaguchi@sun.com">Kohsuke KAWAGUCHI</a>
 */
class LazyTypeIncubator implements XSTypeIncubator { // package local
	
	public LazyTypeIncubator( XSDatatypeExp base, GrammarReader reader ) {
		this.baseType = base;
        this.reader = reader;
	}
	
	/** base object. */
	private final XSDatatypeExp baseType;
	
    private final GrammarReader reader;
    
	/**
	 * applied facets.
	 * Order between facets are possibly significant.
	 */
	private final List facets = new java.util.LinkedList();
	
	public void addFacet( String name, String strValue, ValidationContext context ) {
		facets.add( new Facet(name,strValue,context) );
	}

	public XSDatatypeExp derive( final String nsUri, final String localName ) throws DatatypeException {
        
        // facets might be further added, so remember the size of the facet.
        final int facetSize = facets.size();
        
        if(facetSize==0)    return baseType;
        
        return new XSDatatypeExp(nsUri,localName,reader,new XSDatatypeExp.Renderer(){
            public XSDatatype render( XSDatatypeExp.RenderingContext context )
                    throws DatatypeException {
                
                TypeIncubator ti = new TypeIncubator( baseType.getType(context) );
                
		        Iterator itr = facets.iterator();
		        for( int i=0; i<facetSize; i++ ) {
		        	Facet f = (Facet)itr.next();
		        	ti.addFacet( f.name, f.value, f.context );
		        }
		        return ti.derive(nsUri,localName);
            }
        });
	}
	
	/** store the information about one added facet. */
	private class Facet {
		String name;
		String value;
		ValidationContext context;
		public Facet( String name, String value, ValidationContext context ) {
			this.name=name; this.value=value; this.context=context;
		}
	}
}
