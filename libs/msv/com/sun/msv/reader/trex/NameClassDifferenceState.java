/*
 * @(#)$Id: NameClassDifferenceState.java,v 1.6 2001/05/29 23:06:37 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex;

import com.sun.msv.grammar.NameClass;
import com.sun.msv.grammar.DifferenceNameClass;
import java.util.ArrayList;
import java.util.List;

/**
 * parses &lt;difference&gt; name class.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class NameClassDifferenceState extends NameClassWithChildState {
	protected NameClass castNameClass( NameClass halfCasted, NameClass newChild ) {
		if( halfCasted==null )	return newChild;	// first item
		else return new DifferenceNameClass( halfCasted, newChild );
	}
}
