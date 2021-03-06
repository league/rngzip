/*
 * @(#)$Id: ShortType.java,v 1.16 2001/11/27 01:54:52 kk122374 Exp $
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
 * "short" type.
 * 
 * type of the value object is <code>java.lang.Short</code>.
 * See http://www.w3.org/TR/xmlschema-2/#short for the spec
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ShortType extends IntegerDerivedType {
	public static final ShortType theInstance = new ShortType("short");
	protected ShortType(String typeName) { super(typeName); }
	
	public XSDatatype getBaseType() {
		return IntType.theInstance;
	}
	
	public Object _createValue( String lexicalValue, ValidationContext context ) {
        return load(lexicalValue);
    }
    
    public static Short load( String s ) {
		// Implementation of JDK1.2.2/JDK1.3 is suitable enough
		try {
			return new Short(removeOptionalPlus(s));
		} catch( NumberFormatException e ) {
			return null;
		}
	}
    
    public static String save( Short v ) {
        return v.toString();
    }
    
	public Class getJavaObjectType() {
		return Short.class;
	}
}
