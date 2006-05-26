/*
 * @(#)$Id: XSTypeOwner.java,v 1.2 2002/06/24 19:57:58 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.datatype.xsd;

import org.relaxng.datatype.Datatype;

/**
 * Type owner for XML Schema datatypes.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public interface XSTypeOwner
{
    /**
     * Returns the target namespace URI of newly created simple types.
     * If the context you are using this package doesn't have the notion
     * of the target namespace URI, return the empty string.
     */
    String getTargetNamespaceUri();
    
    void onEndChild( XSDatatypeExp data );
}
