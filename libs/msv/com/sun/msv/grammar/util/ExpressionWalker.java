/*
 * @(#)$Id: ExpressionWalker.java,v 1.6 2001/10/18 23:52:11 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar.util;

import com.sun.msv.grammar.*;

/**
 * Visits all reachable expressions but do nothing.
 * 
 * Note that unless the derived class do something, this implementation
 * will recurse infinitely.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public abstract class ExpressionWalker implements ExpressionVisitorVoid {
		
	public void onRef( ReferenceExp exp ) {
		exp.exp.visit(this);
	}
		
	public void onOther( OtherExp exp ) {
		exp.exp.visit(this);
	}
	
	public void onElement( ElementExp exp ) {
		exp.contentModel.visit(this);
	}
		
	public void onEpsilon() {}
	public void onNullSet() {}
	public void onAnyString() {}
	public void onData( DataExp exp ) {}
	public void onValue( ValueExp exp ) {}
		
	public void onInterleave( InterleaveExp exp ) {
		onBinExp(exp);
	}
		
	public void onConcur( ConcurExp exp ) {
		onBinExp(exp);
	}
			
	public void onChoice( ChoiceExp exp ) {
		onBinExp(exp);
	}
		
	public void onSequence( SequenceExp exp ) {
		onBinExp(exp);
	}
		
	public void onBinExp( BinaryExp exp ) {
		exp.exp1.visit(this);
		exp.exp2.visit(this);
	}
		
	public void onMixed( MixedExp exp ) {
		exp.exp.visit(this);
	}
	
	public void onList( ListExp exp ) {
		exp.exp.visit(this);
	}
	
	public void onOneOrMore( OneOrMoreExp exp ) {
		exp.exp.visit(this);
	}
		
	public void onAttribute( AttributeExp exp ) {
		exp.exp.visit(this);
	}
}
