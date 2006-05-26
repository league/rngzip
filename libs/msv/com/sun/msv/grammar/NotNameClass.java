/*
 * @(#)$Id: NotNameClass.java,v 1.8 2001/07/28 02:50:38 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar;

/**
 * NameClass that acts a not operator.
 * 
 * Actually, A NotNameClass can be represented by using a DifferenceNameClass
 * and AnyNameClass.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public final class NotNameClass extends NameClass {
	public final NameClass child;

	/**
	 * accepts a name if it is not accepted by the child name class.
	 */
	public boolean accepts( String namespaceURI, String localName ) {
		return !child.accepts(namespaceURI,localName);
	}
	
	public Object visit( NameClassVisitor visitor ) { return visitor.onNot(this); }

	public NotNameClass( NameClass child ) {
		this.child = child;
	}
	
	public String toString()	{ return "~"+child.toString(); }
}
