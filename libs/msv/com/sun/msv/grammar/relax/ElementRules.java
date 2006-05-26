/*
 * @(#)$Id: ElementRules.java,v 1.6 2001/05/16 21:33:17 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar.relax;

import com.sun.msv.grammar.*;

/**
 * Set of ElementRule objects that share the label name.
 * 
 * ReferenceExp.exp contains choice of ElementRule objects.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ElementRules extends ReferenceExp implements Exportable {
	
	protected ElementRules( String label, RELAXModule ownerModule ) {
		super(label);
		this.ownerModule = ownerModule;
	}
	
	public boolean equals( Object o ) {
		return this==o;
	}
	
	protected boolean calcEpsilonReducibility() {
		// elementRules are always not epsilon-reducible.
		return false;
	}
	
	public void addElementRule( ExpressionPool pool, ElementRule newRule ) {
		newRule.parent = this;
		if( exp==null )		// the first element
			exp = newRule;
		else
			exp = pool.createChoice(exp,newRule);
	}

	public Object visit( RELAXExpressionVisitor visitor )
	{ return visitor.onElementRules(this); }

	public Expression visit( RELAXExpressionVisitorExpression visitor )
	{ return visitor.onElementRules(this); }
	
	public boolean visit( RELAXExpressionVisitorBoolean visitor )
	{ return visitor.onElementRules(this); }

	public void visit( RELAXExpressionVisitorVoid visitor )
	{ visitor.onElementRules(this); }

	/**
	 * a flag that indicates this elementRule is exported and
	 * therefore accessible from other modules.
	 */
	public boolean exported = false;
	public boolean isExported() { return exported; }
	
	/** RELAXModule object to which this object belongs */
	public final RELAXModule ownerModule;
}
