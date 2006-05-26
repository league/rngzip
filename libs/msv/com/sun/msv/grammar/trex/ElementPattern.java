/*
 * @(#)$Id: ElementPattern.java,v 1.6 2001/05/16 21:33:18 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar.trex;

import com.sun.msv.grammar.*;

/**
 * &lt;element&gt; pattern of TREX.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ElementPattern extends ElementExp {
	public final NameClass nameClass;
	public final NameClass getNameClass() { return nameClass; }
	
	public ElementPattern( NameClass nameClass, Expression contentModel ) {
		super(contentModel,false);
		this.nameClass = nameClass;
	}
}
