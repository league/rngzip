/*
 * @(#)$Id: IDContextProvider.java,v 1.8 2001/09/14 22:04:20 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar;

import org.relaxng.datatype.ValidationContext;
import org.relaxng.datatype.Datatype;

/**
 * ValidationContextProvider that supports limited ID/IDREF implementation.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public interface IDContextProvider extends ValidationContext {
	
	/**
	 * this method is called when a type with ID semantics is matched.
	 * 
	 * It is the callee's responsibility that stores
	 * ID and checks doubly defined ID, if it is necessary.
	 */
	void onID( Datatype datatype, String literal );
}
