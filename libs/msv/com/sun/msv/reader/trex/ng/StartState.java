/*
 * @(#)$Id: StartState.java,v 1.2 2001/10/12 01:37:14 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex.ng;

import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.ReferenceExp;
import com.sun.msv.reader.SequenceState;
import org.xml.sax.Locator;

/**
 * parses &lt;start&gt; declaration.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class StartState extends DefineState {

	protected ReferenceExp getReference() {
		final RELAXNGReader reader = (RELAXNGReader)this.reader;
		
		if( startTag.containsAttribute("name") ) {
			// since the name attribute is allowed to the certain point,
			// it is useful to explicitly raise this as an error.
			reader.reportError( reader.ERR_DISALLOWED_ATTRIBUTE, startTag.qName, "name" );
			return null;
		}
			
		return reader.getGrammar();
	}
}
