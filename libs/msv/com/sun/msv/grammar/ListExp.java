/*
 * @(#)$Id: ListExp.java,v 1.3 2003/01/16 21:51:17 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar;

/**
 * &lt;list&gt; of RELAX NG.
 * 
 * This primitive is not used by RELAX Core,TREX,DTD, and W3C XML Schema.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public final class ListExp extends UnaryExp {
	
	ListExp( Expression exp )	{ super( exp,HASHCODE_LIST ); }
	
	public Object visit( ExpressionVisitor visitor )				{ return visitor.onList(this);	}
	public Expression visit( ExpressionVisitorExpression visitor )	{ return visitor.onList(this); }
	public boolean visit( ExpressionVisitorBoolean visitor )		{ return visitor.onList(this); }
	public void visit( ExpressionVisitorVoid visitor )				{ visitor.onList(this); }

	protected boolean calcEpsilonReducibility() {
		return exp.isEpsilonReducible();
	}
    
    // serialization support
    private static final long serialVersionUID = 1;    
}
