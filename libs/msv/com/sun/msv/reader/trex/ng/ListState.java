/*
 * @(#)$Id: ListState.java,v 1.1 2001/05/31 20:43:38 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex.ng;

import com.sun.msv.reader.SequenceState;
import com.sun.msv.grammar.Expression;

/**
 * state that parses &lt;list&gt; pattern of RELAX NG.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ListState extends SequenceState {
	protected Expression annealExpression( Expression exp ) {
		return reader.pool.createList(exp);
	}
}
