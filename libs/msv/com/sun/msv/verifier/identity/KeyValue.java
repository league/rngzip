package com.sun.msv.verifier.identity;

/*
 * @(#)$Id: KeyValue.java,v 1.2 2001/12/06 04:19:43 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

/**
 * represents multi-field keys.
 * 
 * this class implements equality test and hash code based on
 * the equalities of each item.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
class KeyValue {
	public final Object[] values;
	
	/** source location that this value is found. */
	public final Locator locator;
	
	KeyValue( Object[] values, Locator loc ) {
		this.values = values;
        if(loc==null)   this.locator = null;
        else            this.locator = new LocatorImpl(loc);
	}
	
	public int hashCode() {
		int code = 0;
		for( int i=0; i<values.length; i++ )
			code ^= values[i].hashCode();
		return code;
	}
	
	public boolean equals( Object o ) {
		if(!(o instanceof KeyValue))	return false;
		KeyValue rhs = (KeyValue)o;
		if( values.length!=rhs.values.length )	return false;
		
		for( int i=0; i<values.length; i++ )
			if( !values[i].equals(rhs.values[i]) )	return false;
		
		return true;
	}
}
