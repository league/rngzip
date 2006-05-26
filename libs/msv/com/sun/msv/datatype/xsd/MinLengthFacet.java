/*
 * @(#)$Id: MinLengthFacet.java,v 1.21 2002/06/24 19:57:28 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd;

import org.relaxng.datatype.DatatypeException;
import org.relaxng.datatype.ValidationContext;

/**
 * 'minLength' facet
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class MinLengthFacet extends DataTypeWithValueConstraintFacet {
	public final int minLength;
	
	protected MinLengthFacet( String nsUri, String typeName, XSDatatypeImpl baseType, TypeIncubator facets )
		throws DatatypeException {
		super(nsUri,typeName,baseType,FACET_MINLENGTH,facets);
	
		minLength = facets.getNonNegativeInteger(FACET_MINLENGTH);
		
		// loosened facet check
		DataTypeWithFacet o = baseType.getFacetObject(FACET_MINLENGTH);
		if(o!=null && ((MinLengthFacet)o).minLength > this.minLength )
			throw new DatatypeException( localize( ERR_LOOSENED_FACET,
				FACET_MINLENGTH, o.displayName() ) );
		
		// consistency with maxLength is checked in XSDatatypeImpl.derive method.
	}
	
	public Object _createValue( String literal, ValidationContext context ) {
		Object o = baseType._createValue(literal,context);
		if(o==null || ((Discrete)concreteType).countLength(o)<minLength)	return null;
		return o;
	}
	
	protected void diagnoseByFacet(String content, ValidationContext context) throws DatatypeException {
		Object o = concreteType._createValue(content,context);
		// base type must have accepted this lexical value, otherwise 
		// this method is never called.
		if(o==null)	throw new IllegalStateException();	// assertion
		
		int cnt = ((Discrete)concreteType).countLength(o);
		if(cnt<minLength)
			throw new DatatypeException( DatatypeException.UNKNOWN,
				localize(ERR_MINLENGTH,	new Integer(cnt), new Integer(minLength)) );
	}
}
