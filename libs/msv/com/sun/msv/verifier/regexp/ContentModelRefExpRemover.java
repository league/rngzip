/*
 * @(#)$Id: ContentModelRefExpRemover.java,v 1.3 2001/10/09 00:05:13 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.verifier.regexp;

import com.sun.msv.grammar.*;
import com.sun.msv.grammar.relax.*;
import com.sun.msv.grammar.trex.*;
import java.util.Iterator;
import java.util.Set;

/**
 * Non-recursive ReferenceExpRemover with a cache.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ContentModelRefExpRemover {
	
//	public static Expression remove( Expression exp, ExpressionPool pool ) {
//		return exp.getExpandedExp(pool);
//	}
	
	
	// the class that does the actual job.
	private static class Remover extends ExpressionCloner {
		public Remover( ExpressionPool pool ) { super(pool); }
	
		public Expression onElement( ElementExp exp ) {
			return exp;
		}
	
		public Expression onAttribute( AttributeExp exp ) {
			Expression content = exp.exp.visit(this);
			if( content==Expression.nullSet )
				return Expression.nullSet;	// this attribute is not allowed
			else
				return pool.createAttribute( exp.nameClass, content );
		}
	
		public Expression onRef( ReferenceExp exp ) {
			return exp.exp.visit(this);
		}
	
		public Expression onOther( OtherExp exp ) {
			return exp.exp.visit(this);
		}
	}
}
