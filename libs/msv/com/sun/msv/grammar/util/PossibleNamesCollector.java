/*
 * @(#)$Id: PossibleNamesCollector.java,v 1.1 2001/06/29 02:50:42 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar.util;

import com.sun.msv.grammar.*;
import com.sun.msv.util.StringPair;
import java.util.Set;

/**
 * computes the possible names.
 * 
 * <p>
 * See <a href="http://lists.oasis-open.org/ob/htsearch?config=lists_oasis-open_org&restrict=relax-ng%2F&method=and&sort=score&words=possibleNames">
 * the description</a>.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@sun.com">Kohsuke KAWAGUCHI</a>
 */
public class PossibleNamesCollector implements NameClassVisitor {
	
	/**
	 * computes all possibile names for this name class, and returns
	 * the set of {@link StringPair}.
	 */
	public static Set calc( NameClass nc ) {
		PossibleNamesCollector col = new PossibleNamesCollector();
		nc.visit(col);
		return col.names;
	}
	
	
	public static final String MAGIC = "\u0000";
	private static final StringPair pairForAny = new StringPair( MAGIC, MAGIC );
	
	/** this set will receive all possible names. */
	private Set names = new java.util.HashSet();
	
	public Object onChoice( ChoiceNameClass nc ) {
		nc.nc1.visit(this);
		nc.nc2.visit(this);
		return null;
	}
	public Object onAnyName( AnyNameClass nc ) {
		names.add( pairForAny );
		return null;
	}
	public Object onSimple( SimpleNameClass nc ) {
		names.add( new StringPair( nc.namespaceURI, nc.localName ) );
		return null;
	}
	public Object onNsName( NamespaceNameClass nc ) {
		names.add( new StringPair( nc.namespaceURI, MAGIC ) );
		return null;
	}
	public Object onNot( NotNameClass nc ) {
		names.add( pairForAny );
		nc.child.visit(this);
		return null;
	}
	public Object onDifference( DifferenceNameClass nc ) {
		nc.nc1.visit(this);
		nc.nc2.visit(this);
		return null;
	}
};
