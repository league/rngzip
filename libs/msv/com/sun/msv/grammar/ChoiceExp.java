/*
 * @(#)$Id: ChoiceExp.java,v 1.5 2001/05/16 21:33:15 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar;

/**
 * A|B.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public final class ChoiceExp extends BinaryExp {
	
	ChoiceExp( Expression left, Expression right ) {
		super(left,right,HASHCODE_CHOICE);
	}
	
	public Object visit( ExpressionVisitor visitor )				{ return visitor.onChoice(this); }
	public Expression visit( ExpressionVisitorExpression visitor )	{ return visitor.onChoice(this); }
	public boolean visit( ExpressionVisitorBoolean visitor )		{ return visitor.onChoice(this); }
	public void visit( ExpressionVisitorVoid visitor )				{ visitor.onChoice(this); }

	protected boolean calcEpsilonReducibility() {
		return exp1.isEpsilonReducible() || exp2.isEpsilonReducible();
	}
}
