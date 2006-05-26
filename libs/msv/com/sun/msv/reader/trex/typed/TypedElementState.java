/*
 * @(#)$Id: TypedElementState.java,v 1.5 2001/05/01 18:13:16 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex.typed;

import com.sun.msv.grammar.Expression;
import com.sun.msv.reader.trex.ElementState;
import com.sun.msv.grammar.trex.typed.TypedElementPattern;

/**
 * reads &lt;element&gt; element with 'label' annotation.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class TypedElementState extends ElementState
{
	protected Expression annealExpression( Expression contentModel )
	{
		final String label = startTag.getAttribute( TypedTREXGrammarInterceptor.LABEL_NAMESPACE, "label" );
		if( label==null )
			return super.annealExpression( contentModel );
		else
			return new TypedElementPattern( nameClass, contentModel, label );
	}
}
