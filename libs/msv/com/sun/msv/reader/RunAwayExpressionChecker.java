/*
 * @(#)$Id: RunAwayExpressionChecker.java,v 1.18 2002/09/27 04:14:18 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader;

import com.sun.msv.grammar.*;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import org.xml.sax.Locator;

/**
 * makes sure that the expression does not run away.
 * 
 * "run-away" expressions are expressions like this.
 * 
 * &lt;hedgeRule label="foo" /&gt;
 *   &lt;hedgeRef label="foo" /&gt;
 * &lt;/hedgeRule&gt;
 * 
 * Apparently, those expressions cannot be expressed in string regular expression.
 * Therefore run-away expressions are prohibited in both RELAX and TREX.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@sun.com">Kohsuke KAWAGUCHI</a>
 */
public class RunAwayExpressionChecker implements ExpressionVisitorVoid
{
	/** this exception is thrown to abort check when a error is found. */
	protected static final RuntimeException eureka = new RuntimeException();
		
	/** set of ElementExps which are already confirmed as being not a run-away exp. */
	private final Set testedExps = new java.util.HashSet();
	
	/** Expressions which are used as the content model of current element. */
	private Set contentModel = new java.util.HashSet();
	
	/** 
	 * visited Expressions.
	 * this information is useful for the user to figure out where did they make a mistake.
	 */
	private Stack refStack = new Stack();
	
    /**
     * Queue of unchecked element exps.
     */
    private Stack unprocessedElementExps = new Stack();
    
	private final GrammarReader reader;
	
	protected RunAwayExpressionChecker( GrammarReader reader ) { this.reader = reader; }

	private void check( Expression exp ) {
		try {
            exp.visit(this);
            
            while(!unprocessedElementExps.isEmpty()) {
                contentModel.clear();
                refStack.clear();
                ElementExp e = (ElementExp)unprocessedElementExps.pop();
                e.contentModel.visit(this);
            }
		} catch( RuntimeException e ) {
			if(e!=eureka)	throw e;
		}
	}
	
	public static void check( GrammarReader reader, Expression exp ) {
		new RunAwayExpressionChecker(reader).check(exp);
	}
	
	public void onAttribute( AttributeExp exp ) {
		enter(exp);
		exp.exp.visit(this);
		leave(exp);
	}
	public void onConcur( ConcurExp exp )				{ binaryVisit(exp);	}
	public void onInterleave( InterleaveExp exp )		{ binaryVisit(exp);	}
	public void onSequence( SequenceExp exp )			{ binaryVisit(exp); }
	public void onChoice( ChoiceExp exp )				{ binaryVisit(exp); }
	public void onOneOrMore( OneOrMoreExp exp )			{ unaryVisit(exp); }
	public void onMixed( MixedExp exp )					{ unaryVisit(exp); }
	public void onList( ListExp exp )					{ unaryVisit(exp); }
	public void onEpsilon()								{}
	public void onNullSet()								{}
	public void onAnyString()							{}
	public void onData( DataExp exp )					{}
	public void onValue( ValueExp exp )					{}
	
	protected final void binaryVisit( BinaryExp exp ) {
		enter(exp);
		exp.exp1.visit(this);
		exp.exp2.visit(this);
		leave(exp);
	}
	protected final void unaryVisit( UnaryExp exp ) {
		enter(exp);
		exp.exp.visit(this);
		leave(exp);
	}
	
	private void enter( Expression exp ) {
		if(contentModel.contains(exp)) {
			// this indicates that we have reached the same expression object
			// without visiting any ElementExp.
			// so this one is a run-away expression.
				
			// check stack to find actual sequence of reference.
			String s = "";
			int i = refStack.indexOf(exp);
			int sz = refStack.size();
			
			Vector locs = new Vector();
			
			for( ; i<sz; i++ ) {
				if( refStack.elementAt(i) instanceof ReferenceExp ) {
					ReferenceExp e = (ReferenceExp)refStack.elementAt(i);
					if( e.name==null )	continue;	// skip anonymous ref.
					
					if( s.length()!=0 )	 s += " > ";
					s += e.name;
					
					Locator loc = reader.getDeclaredLocationOf(e);
					if(loc==null)	continue;
					locs.add(loc);
				}
			}
				
			reader.reportError( (Locator[])locs.toArray(new Locator[0]), GrammarReader.ERR_RUNAWAY_EXPRESSION, new Object[]{s} );
			
			// abort further run-away check.
			// usually, run-away expression error occurs by use of hedgeRules,
			// and those rules tend to be shared among multiple elementRules.
			
			// So it is highly likely that further check will generate too many errors.
			throw eureka;
		}
		contentModel.add(exp);
		refStack.push(exp);
	}
	private void leave( Expression exp ) {
		contentModel.remove(exp);
		refStack.pop();
	}

	public void onRef( ReferenceExp exp ) {
		enter(exp);
		if( !testedExps.contains(exp) ) {
			testedExps.add(exp);
			exp.exp.visit(this);
		}
		leave(exp);
	}
	public void onOther( OtherExp exp ) {
		enter(exp);
		exp.exp.visit(this);
		leave(exp);
	}
	
	public void onElement( ElementExp exp )
	{
		if( !testedExps.add(exp) )
			// this expression is already tested. no need to test it again.
			return;
        
        unprocessedElementExps.push(exp);   // process this later.
	}
}
