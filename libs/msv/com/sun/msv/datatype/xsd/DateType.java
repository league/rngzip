/*
 * @(#)$Id: DateType.java,v 1.19 2001/08/14 21:47:12 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd;

import com.sun.msv.datatype.SerializationContext;
import com.sun.msv.datatype.xsd.datetime.ISO8601Parser;
import com.sun.msv.datatype.xsd.datetime.IDateTimeValueType;
import com.sun.msv.datatype.xsd.datetime.BigDateTimeValueType;
import org.relaxng.datatype.ValidationContext;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;

/**
 * "date" type.
 * 
 * type of the value object is {@link IDateTimeValueType}.
 * See http://www.w3.org/TR/xmlschema-2/#dateTime for the spec
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class DateType extends DateTimeBaseType {
	
	public static final DateType theInstance = new DateType();
	private DateType() { super("date"); }

	protected void runParserL( ISO8601Parser p ) throws Exception {
		p.dateTypeL();
	}

	protected IDateTimeValueType runParserV( ISO8601Parser p ) throws Exception {
		return p.dateTypeV();
	}
	
	public String convertToLexicalValue( Object value, SerializationContext context ) {
		if(!(value instanceof IDateTimeValueType))
			throw new IllegalArgumentException();
		
		BigDateTimeValueType bv = ((IDateTimeValueType)value).getBigValue();
		return	formatYear(bv.getYear())+"-"+
				formatTwoDigits(bv.getMonth(),1)+"-"+
				formatTwoDigits(bv.getDay(),1)+
				formatTimeZone(bv.getTimeZone());
	}

	public String serializeJavaObject( Object value, SerializationContext context ) {
		if(!(value instanceof Calendar))	throw new IllegalArgumentException();
		Calendar cal = (Calendar)value;
		
		StringBuffer result = new StringBuffer();
		result.append(formatYear(cal.get(cal.YEAR)));
		result.append('-');
		result.append(formatTwoDigits(cal.get(cal.MONTH)+1));
		result.append('-');
		result.append(formatTwoDigits(cal.get(cal.DAY_OF_MONTH)));
		result.append(formatTimeZone(cal));
		
		return result.toString();
	}
}
