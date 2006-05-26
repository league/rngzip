/*
 * @(#)$Id: ElementState.java,v 1.2 2001/10/31 19:56:22 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex.ng;

import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.trex.ElementPattern;

/**
 * parses &lt;element&gt; pattern.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ElementState extends com.sun.msv.reader.trex.ElementState {
	
	private boolean previousDirectReference;
	
	protected void startSelf() {
		super.startSelf();
		
		// set directReference to false.
		previousDirectReference = ((RELAXNGReader)reader).directRefernce;
		((RELAXNGReader)reader).directRefernce = false;
	}
	
	protected void endSelf() {
		final RELAXNGReader reader = (RELAXNGReader)this.reader;
		
		reader.directRefernce = previousDirectReference;
		super.endSelf();

		reader.restrictionChecker.checkNameClass(nameClass);
	}
}
