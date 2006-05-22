/*
 * @(#)$Id: KeyConstraint.java,v 1.2 2003/01/16 21:51:21 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar.xmlschema;

/**
 * key constraint.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class KeyConstraint extends IdentityConstraint {
	public KeyConstraint( String namespaceURI, String localName, XPath[] selector, Field[] fields ) {
		super(namespaceURI,localName,selector,fields);
	}
    
    // serialization support
    private static final long serialVersionUID = 1;    
}
