/*
 * @(#)$Id: NameClassAnyNameState.java,v 1.6 2001/06/19 22:34:17 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex;

import com.sun.msv.grammar.NameClass;
import com.sun.msv.grammar.AnyNameClass;

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
