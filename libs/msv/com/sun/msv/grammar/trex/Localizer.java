/*
 * @(#)$Id: Localizer.java,v 1.5 2001/05/16 21:33:18 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar.trex;

/**
 * formats messages by using a resource file.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
class Localizer {
	public static String localize( String prop, Object[] args ) {
		return java.text.MessageFormat.format(
			java.util.ResourceBundle.getBundle("com.sun.msv.grammar.trex.Messages").getString(prop),
			args );
	}
	
	public static String localize( String prop ) {
		return localize( prop, null );
	}
	
	public static String localize( String prop, Object arg1 ) {
		return localize( prop, new Object[]{arg1} );
	}
}
