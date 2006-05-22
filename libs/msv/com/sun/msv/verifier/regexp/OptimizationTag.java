/*
 * @(#)$Id: OptimizationTag.java,v 1.8 2003/01/09 21:00:22 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.verifier.regexp;

import java.util.Map;

import com.sun.msv.grammar.ElementExp;
import com.sun.msv.grammar.Expression;

/**
 * this object will be added to Expression.verifierTag
 * to speed up typical validation.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
final class OptimizationTag
{
	/** cached value of string care level.
	 * See Acceptor.getStringCareLevel for meanings of value.
	 */
	int stringCareLevel = STRING_NOTCOMPUTED;
	
	/** a value indicates that stringCareLevel has not computed yet. */
	public static final int STRING_NOTCOMPUTED = -1;
	
	/**
	 * map from element to residual(exp,ElementToken(element))
	 * 
	 * this map is not applicable when the ElementToken represents
	 * more than one element. Because of 'concur' operator.
	 * 
	 * In RELAX, 
	 *  residual(exp,elem1|elem2) = residual(exp,elem1) | residual(exp,elem2)
	 * 
	 * Since it is possible for multiple threads to access the same OptimizationTag
	 * concurrently, it has to be serialized.
	 */
	final Map simpleElementTokenResidual = new java.util.Hashtable();
	
	protected static final class OwnerAndCont
	{
		final ElementExp owner;
		final Expression continuation;
		public OwnerAndCont( ElementExp owner, Expression cont )
		{ this.owner=owner; this.continuation=cont; }
	};
	/** map from (namespaceURI,tagName) pair to OwnerAndContinuation. */
	final Map transitions = new java.util.Hashtable();

	/** AttributePruner.prune(exp) */
	Expression attributePrunedExpression;
	
//	/** a flag that indicates this expression doesn't have any attribute node.
//	 * 
//	 * null means unknown.
//	 */
//	Boolean isAttributeFree;
}
