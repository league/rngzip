/*
 * @(#)$Id: PositiveIntegerType.java,v 1.14 2001/08/14 22:17:57 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd;

import org.relaxng.datatype.ValidationContext;

/**
 * "positiveInteger" type.
 * 
 * type of the value object is {@link IntegerValueType}.
 * See http://www.w3.org/TR/xmlschema-2/#positiveInteger for the spec
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class PositiveIntegerType extends IntegerType {
	
	public static final PositiveIntegerType theInstance = new PositiveIntegerType();
	private PositiveIntegerType() { super("positiveInteger"); }
	
	final public XSDatatype getBaseType() {
		return NonNegativeIntegerType.theInstance;
	}
	
	public Object _createValue( String lexicalValue, ValidationContext context ) {
		Object o = super._createValue(lexicalValue,context);
		if(o==null)		return null;
		
		final IntegerValueType v = (IntegerValueType)o;
		if( !v.isPositive() )	return null;
		return v;
	}
}
