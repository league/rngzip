/*
 * @(#)$Id: MinInclusiveFacet.java,v 1.10 2002/06/24 19:57:28 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd;

import org.relaxng.datatype.DatatypeException;

/**
 * 'minInclusive' facet
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class MinInclusiveFacet extends RangeFacet {
	protected MinInclusiveFacet( String nsUri, String typeName, XSDatatypeImpl baseType, TypeIncubator facets )
		throws DatatypeException {
		super( nsUri, typeName, baseType, FACET_MININCLUSIVE, facets );
	}
	
	protected final boolean rangeCheck( int r ) {
		return r==Comparator.LESS || r==Comparator.EQUAL;
	}
}
