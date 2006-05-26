/*
 * @(#)$Id: GrammarState.java,v 1.1 2001/10/12 23:37:57 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex.ng;

import com.sun.msv.util.StartTagInfo;
import com.sun.msv.reader.State;
import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.trex.TREXGrammar;

/**
 * parses &lt;grammar&gt; element.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class GrammarState extends com.sun.msv.reader.trex.GrammarState {
	protected void startSelf() {
		super.startSelf();
		
		final RELAXNGReader reader = (RELAXNGReader)this.reader;
		
		// memorize this reference as a direct reference.
		if( reader.currentNamedPattern!=null ) {
			if(reader.directRefernce)
				reader.currentNamedPattern.directRefs.add(newGrammar);
			else
				reader.currentNamedPattern.indirectRefs.add(newGrammar);
		}
	}
}
