/*
 * @(#)$Id: UniqueConstraint.java,v 1.1 2001/05/15 21:52:36 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar.xmlschema;

/**
 * unique constraint.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class UniqueConstraint extends IdentityConstraint {
	public UniqueConstraint( String namespaceURI, String localName, XPath[] selector, Field[] fields ) {
		super(namespaceURI,localName,selector,fields);
	}
}
