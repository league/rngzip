/*
 * @(#)$Id: StringPair.java,v 1.9 2002/07/25 00:17:04 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.util;

import com.sun.msv.grammar.SimpleNameClass;

/**
 * pair of Strings.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public final class StringPair implements java.io.Serializable
{
	public final String namespaceURI;
	public final String localName;
	
	public StringPair( SimpleNameClass name ) { this(name.namespaceURI, name.localName); }
	public StringPair( String ns, String ln ) {
        // assertion check
        if(ns==null)
            throw new InternalError("namespace URI is null");
        if(ln==null)
            throw new InternalError("local name is null");
        
        namespaceURI=ns;
        localName=ln;
    }
	
	public boolean equals( Object o )
	{
		if(!(o instanceof StringPair))	return false;
		
		return namespaceURI.equals(((StringPair)o).namespaceURI)
			&& localName.equals(((StringPair)o).localName);
	}
	public int hashCode() { return namespaceURI.hashCode()^localName.hashCode(); }
	
	public String toString() {
		return "{"+namespaceURI+"}"+localName;
	}
}
