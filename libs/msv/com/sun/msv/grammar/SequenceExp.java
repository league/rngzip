/*
 * @(#)$Id: SequenceExp.java,v 1.6 2001/06/15 03:24:21 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar;

/**
 * A,B. concatenation operator of the regular expression.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public final class SequenceExp extends BinaryExp {
	
	SequenceExp( Expression left, Expression right )	{ super(left,right,HASHCODE_SEQUENCE); }
	
	public Object visit( ExpressionVisitor visitor )				{ return visitor.onSequence(this); }
	public Expression visit( ExpressionVisitorExpression visitor )	{ return visitor.onSequence(this); }
	public boolean visit( ExpressionVisitorBoolean visitor )		{ return visitor.onSequence(this); }
	public void visit( ExpressionVisitorVoid visitor )				{ visitor.onSequence(this); }

	protected boolean calcEpsilonReducibility() {
		return exp1.isEpsilonReducible() && exp2.isEpsilonReducible();
	}
}
