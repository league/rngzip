/*
 * @(#)$Id: OptionalState.java,v 1.5 2001/05/29 23:06:37 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex;

import com.sun.msv.grammar.Expression;
import com.sun.msv.reader.SequenceState;

/**
 * parses &lt;optional&gt; pattern.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class OptionalState extends SequenceState {
	protected Expression annealExpression( Expression exp ) {
		return reader.pool.createOptional(exp);
	}
}
