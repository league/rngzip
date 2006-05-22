/*
 * @(#)$Id: MaxExclusiveFacet.java,v 1.12 2003/02/12 19:58:14 kk122374 Exp $
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
 * 'maxExclusive' facet
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class MaxExclusiveFacet extends RangeFacet {
    protected MaxExclusiveFacet( String nsUri, String typeName, XSDatatypeImpl baseType, Number limit, boolean _isFixed )
        throws DatatypeException {
        super( nsUri, typeName, baseType, FACET_MAXEXCLUSIVE, limit, _isFixed );
    }
	
	protected final boolean rangeCheck( int r ) {
		return r==Comparator.GREATER;
	}

    // serialization support
    private static final long serialVersionUID = 1;    
}
