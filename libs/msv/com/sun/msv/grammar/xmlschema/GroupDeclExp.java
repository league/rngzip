/*
 * @(#)$Id: GroupDeclExp.java,v 1.5 2001/05/01 18:13:00 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar.xmlschema;

/**
 * model group declaration.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class GroupDeclExp extends RedefinableExp {
	
	public GroupDeclExp( String typeLocalName ) {
		super(typeLocalName);
	}
	
	/** clone this object. */
	public RedefinableExp getClone() {
		RedefinableExp exp = new GroupDeclExp(super.name);
		exp.redefine(this);
		return exp;
	}
}
