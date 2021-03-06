/*
 * @(#)$Id: NamespaceNameClass.java,v 1.8 2001/07/28 02:50:38 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar;

/**
 * NameClass that matchs any names in a particular namespace.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class NamespaceNameClass extends NameClass {
	public final String	namespaceURI;
	
	/**
	 * accepts a name if its namespace URI matches to the namespaceURI field.
	 */
	public boolean accepts( String namespaceURI, String localName ) {
		if( NAMESPACE_WILDCARD==namespaceURI )	return true;
		return this.namespaceURI.equals(namespaceURI);
	}
	
	public Object visit( NameClassVisitor visitor ) { return visitor.onNsName(this); }
	
	public NamespaceNameClass( String namespaceURI ) {
		this.namespaceURI	= namespaceURI;
	}
	
	public String toString() {
		return namespaceURI+":*";
	}
}
