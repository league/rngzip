/*
 * @(#)$Id: StringRecoveryToken.java,v 1.12 2001/10/19 23:59:21 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.verifier.regexp;

import com.sun.msv.grammar.DataExp;
import com.sun.msv.grammar.ValueExp;
import com.sun.msv.grammar.ListExp;
import com.sun.msv.util.DatatypeRef;
import java.util.Set;

/**
 * special StringToken that acts as a wild card.
 * 
 * This object is used for error recovery. It collects all TypedStringExps
 * that ate the token.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
class StringRecoveryToken extends StringToken {
	
	StringRecoveryToken( StringToken base ) {
		this( base, new java.util.HashSet() );
	}
	
	StringRecoveryToken( StringToken base, Set failedExps ) {
		super( base.resCalc, base.literal, base.context, null );
		this.failedExps = failedExps;
	}
	
	/**
	 * TypedStringExps and ListExps that
	 * rejected this token are collected into this set.
	 */
	final Set failedExps;
	
	public boolean match( DataExp exp ) {
		if( super.match(exp) )
			return true;
		
		// this datatype didn't accept me. so record it for diagnosis.
		failedExps.add( exp );
		return true;
	}
	
	public boolean match( ValueExp exp ) {
		if( super.match(exp) )
			return true;
		
		// this datatype didn't accept me. so record it for diagnosis.
		failedExps.add( exp );
		return true;
	}
	
	public boolean match( ListExp exp ) {
		super.match(exp);
		return true;
	}
		
	protected StringToken createChildStringToken( String literal, DatatypeRef dtRef ) {
		return new StringRecoveryToken(
			new StringToken( resCalc, literal, context, dtRef ) );
	}

}
