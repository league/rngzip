/*
 * @(#)$Id: QnameValueType.java,v 1.10 2002/07/31 21:43:49 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd;

/** value object of QName.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class QnameValueType implements java.io.Serializable {
	public final String namespaceURI;
	public final String localPart;
	
	public boolean equals( Object o ) {
		QnameValueType rhs = (QnameValueType)o;
		
		return namespaceURI.equals(rhs.namespaceURI) && localPart.equals(rhs.localPart);
	}
	
	public int hashCode() {
		return namespaceURI.hashCode()+localPart.hashCode();
	}
	
	public String toString() {
		return "{"+namespaceURI+"}:"+localPart;
	}
	
	public QnameValueType( String uri, String localPart ) {
		this.namespaceURI	= uri;
		this.localPart		= localPart;
	}
}
