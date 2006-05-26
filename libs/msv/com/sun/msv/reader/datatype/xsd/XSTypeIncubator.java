/*
 * @(#)$Id: XSTypeIncubator.java,v 1.3 2002/10/06 18:07:05 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.datatype.xsd;

import org.relaxng.datatype.DatatypeException;
import org.relaxng.datatype.ValidationContext;

/**
 * Interface for the type incubator.
 * <p>
 * One layer of abstraction is necessary
 * to support the lazy type construction.
 */
public interface XSTypeIncubator
{
    void addFacet( String name, String value, ValidationContext context )
         throws DatatypeException;
    
    XSDatatypeExp derive( String newTypeNameUri, String newLocalName )
        throws DatatypeException;
}
