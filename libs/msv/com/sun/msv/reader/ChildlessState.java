/*
 * @(#)$Id: ChildlessState.java,v 1.5 2001/05/29 22:50:31 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader;

import com.sun.msv.util.StartTagInfo;

/**
 * state that has no children.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ChildlessState extends SimpleState {
	protected final State createChildState( StartTagInfo tag ) {
		return null;
	}
}
	
	
