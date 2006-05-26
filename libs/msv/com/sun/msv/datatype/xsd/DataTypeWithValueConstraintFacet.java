/*
 * @(#)$Id: DataTypeWithValueConstraintFacet.java,v 1.14 2002/06/24 19:57:27 kk122374 Exp $
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
 * base class for facets which constrain value space.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
abstract class DataTypeWithValueConstraintFacet extends DataTypeWithFacet {
	
	DataTypeWithValueConstraintFacet(
		String nsUri, String typeName, XSDatatypeImpl baseType, String facetName, TypeIncubator facets )
		throws DatatypeException {
	
		super( nsUri, typeName, baseType, facetName, facets );
	}
	
	final protected boolean needValueCheck() { return true; }
	
	protected final boolean checkFormat( String literal, ValidationContext context ) {
		return _createValue(literal,context)!=null;
	}
}
