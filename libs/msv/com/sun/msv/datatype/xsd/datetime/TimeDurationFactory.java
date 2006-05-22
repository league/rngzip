/*
 * @(#)$Id: TimeDurationFactory.java,v 1.8 2003/01/07 00:17:06 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd.datetime;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utility functions to create TimeDurationValueType objects.
 * 
 * @author Kohsuke KAWAGUCHI
 */
public class TimeDurationFactory {
	public static ITimeDurationValueType create(
		Number year, Number month, Number day, Number hour, Number minute, Number mSecond ) {
		// TODO : support SmallTimeDurationValue
		
		BigDecimal second;
		
		if( mSecond==null )	second=null;
		else
		if( mSecond instanceof BigInteger )
			second = ((BigDecimal)mSecond).movePointLeft(3);
		else
			second = new BigDecimal(mSecond.toString()).movePointLeft(3);
		
		return new BigTimeDurationValueType(
			convertToBigInteger(year),
			convertToBigInteger(month),
			convertToBigInteger(day),
			convertToBigInteger(hour),
			convertToBigInteger(minute),
			second );
	}
	
	private static BigInteger convertToBigInteger( Number n ) {
		if(n==null)						return null;
		if(n instanceof BigInteger)		return (BigInteger)n;
		else							return new BigInteger(n.toString());
	}
}
