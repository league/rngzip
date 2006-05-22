/*
 * @(#)$Id: NameClassAnyNameState.java,v 1.7 2003/01/09 21:00:09 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex;

import com.sun.msv.grammar.AnyNameClass;
import com.sun.msv.grammar.NameClass;

/**
 * parses &lt;anyName&gt; name class.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class NameClassAnyNameState extends NameClassWithoutChildState {
	protected NameClass makeNameClass() {
		return AnyNameClass.theInstance;
	}
}
