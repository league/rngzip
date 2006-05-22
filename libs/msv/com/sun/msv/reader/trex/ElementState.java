/*
 * @(#)$Id: ElementState.java,v 1.5 2001/09/14 22:04:24 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex;

import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.trex.ElementPattern;

/**
 * parses &lt;element&gt; pattern.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ElementState extends NameClassAndExpressionState {
	protected Expression annealExpression( Expression contentModel ) {
		ElementPattern e = new ElementPattern( nameClass, contentModel );
		reader.setDeclaredLocationOf(e);
		return e;
	}
}
