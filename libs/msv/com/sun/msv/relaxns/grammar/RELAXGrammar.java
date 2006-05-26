/*
 * @(#)$Id: RELAXGrammar.java,v 1.2 2001/05/01 18:13:23 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.relaxns.grammar;

import java.util.Map;
import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.ExpressionPool;
import com.sun.msv.grammar.Grammar;
import org.iso_relax.dispatcher.IslandSchema;

/**
 * "Grammar" of RELAX Namespace.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class RELAXGrammar implements Grammar {
	
	/**
	 * map from namespace URI to IslandSchema.
	 * All modules are stored in this map.
	 * 
	 * @see IslandSchema
	 */
	public final Map moduleMap = new java.util.HashMap();
	
	/** top-level expression */
	public Expression topLevel;
	public Expression getTopLevel() { return topLevel; }
	
	/** expression pool that was used to create these objects */
	public final ExpressionPool pool;
	public ExpressionPool getPool() { return pool; }
	
	public RELAXGrammar( ExpressionPool pool ) { this.pool = pool; }
}
