/*
 * @(#)$Id: DataTypeWithLexicalConstraintFacet.java,v 1.14 2002/06/24 19:57:27 kk122374 Exp $
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

/**
 * base class for facets which constrains lexical space of data
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
abstract class DataTypeWithLexicalConstraintFacet extends DataTypeWithFacet {
	
	DataTypeWithLexicalConstraintFacet(
		String nsUri, String typeName, XSDatatypeImpl baseType, String facetName, TypeIncubator facets )
		throws DatatypeException {
		super( nsUri, typeName, baseType, facetName, facets );
	}
	
	// this class does not perform any lexical check.
	protected final boolean checkFormat( String literal, ValidationContext context ) {
		if(!baseType.checkFormat(literal,context))	return false;
		return checkLexicalConstraint(literal);
	}
	
	public final Object _createValue( String literal, ValidationContext context ) {
		Object o = baseType._createValue(literal,context);
		if(o!=null && !checkLexicalConstraint(literal) )	return null;
		return o;
	}

	protected abstract boolean checkLexicalConstraint( String literal );
}
