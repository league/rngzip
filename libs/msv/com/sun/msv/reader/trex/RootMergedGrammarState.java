/*
 * @(#)$Id: RootMergedGrammarState.java,v 1.7 2001/07/26 20:43:23 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex;

import com.sun.msv.util.StartTagInfo;
import com.sun.msv.grammar.Expression;
import com.sun.msv.reader.State;
import com.sun.msv.reader.SimpleState;
import com.sun.msv.reader.ExpressionOwner;

/**
 * parses root state of a merged grammar.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class RootMergedGrammarState extends SimpleState implements ExpressionOwner {
	protected State createChildState( StartTagInfo tag ) {
		// expects "grammar" element only, and creates MergeGrammarState
		if( tag.localName.equals("grammar") )	return new DivInGrammarState();
		return null;
	}
	
	public void onEndChild(Expression exp) {
	}
}
