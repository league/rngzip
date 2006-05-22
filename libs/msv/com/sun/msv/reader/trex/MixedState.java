/*
 * @(#)$Id: MixedState.java,v 1.4 2001/05/01 18:13:13 Bear Exp $
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
 * parses &lt;mixed&gt; pattern.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class MixedState extends SequenceState
{
	protected Expression annealExpression( Expression exp )
	{
		return reader.pool.createMixed(exp);
	}
}
