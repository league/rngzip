/*
 * @(#)$Id: WhiteSpaceFacet.java,v 1.17 2002/06/24 19:57:29 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd;

import org.relaxng.datatype.ValidationContext;
import org.relaxng.datatype.DatatypeException;
import org.relaxng.datatype.ValidationContext;

/**
 * whiteSpace facet validator
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class WhiteSpaceFacet extends DataTypeWithFacet {
	
	WhiteSpaceFacet( String nsUri, String typeName, XSDatatypeImpl baseType, TypeIncubator facets )
		throws DatatypeException {
		super(nsUri,typeName, baseType, FACET_WHITESPACE, facets,
			WhiteSpaceProcessor.get( (String)facets.getFacet(FACET_WHITESPACE)) );
		
		// loosened facet check
		if( baseType.whiteSpace.tightness() > this.whiteSpace.tightness() ) {
			XSDatatype d;
			d=baseType.getFacetObject(FACET_WHITESPACE);
			if(d==null)	d = getConcreteType();
			
			throw new DatatypeException( localize(
				ERR_LOOSENED_FACET,	FACET_WHITESPACE, d.displayName() ));
		}
		
		// consistency with minLength/maxLength is checked in XSDatatypeImpl.derive method.
	}
	
	protected boolean checkFormat( String content, ValidationContext context ) {
		return baseType.checkFormat(content,context);
	}
	public Object _createValue( String content, ValidationContext context ) {
		return baseType._createValue(content,context);
	}
	
	/** whiteSpace facet never constrain anything */
	protected void diagnoseByFacet(String content, ValidationContext context) {
		;
	}
}
