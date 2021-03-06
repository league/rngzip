/*
 * @(#)$Id: AnyAttributeState.java,v 1.11 2001/10/10 23:19:46 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.xmlschema;

import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.NameClass;
import com.sun.msv.grammar.ReferenceExp;
import com.sun.msv.grammar.ReferenceContainer;
import com.sun.msv.grammar.xmlschema.XMLSchemaSchema;
import com.sun.msv.grammar.xmlschema.AttributeDeclExp;
import com.sun.msv.grammar.xmlschema.AttributeWildcard;
import com.sun.msv.grammar.xmlschema.AttWildcardExp;
import com.sun.msv.reader.State;
import com.sun.msv.reader.GrammarReader;
import java.util.StringTokenizer;
import java.util.Iterator;

/**
 * used to parse &lt;anyAttribute &gt; element.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class AnyAttributeState extends AnyState {

	protected Expression createExpression( final String namespace, final String process ) {
		final XMLSchemaReader reader = (XMLSchemaReader)this.reader;
		final XMLSchemaSchema currentSchema = reader.currentSchema;

		int mode;
		
		if(process.equals("skip"))		mode = AttributeWildcard.SKIP;
		else
		if(process.equals("lax"))		mode = AttributeWildcard.LAX;
		else
		if(process.equals("strict"))	mode = AttributeWildcard.STRICT;
		else {
			reader.reportError( reader.ERR_BAD_ATTRIBUTE_VALUE, "processContents", process );
			mode = AttributeWildcard.SKIP;
		}
		
		((AnyAttributeOwner)parentState).setAttributeWildcard(
			new AttributeWildcard( getNameClass(namespace,currentSchema), mode ) );
		
		return Expression.epsilon;
	}

	protected NameClass getNameClassFrom( ReferenceExp exp ) {
		return ((AttributeDeclExp)exp).self.nameClass;
	}
}
