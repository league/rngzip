/*
 * @(#)$Id: IncludeModuleState.java,v 1.4 2002/03/04 02:12:04 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.relax.core;

import com.sun.msv.reader.ChildlessState;
import com.sun.msv.reader.AbortException;

/**
 * parses &lt;include&gt; element of RELAX Core.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class IncludeModuleState extends ChildlessState
{
	protected void startSelf() {
		super.startSelf();
	
		final String href = startTag.getAttribute("moduleLocation");

		if(href==null) {
            // name attribute is required.
			reader.reportError( RELAXCoreReader.ERR_MISSING_ATTRIBUTE,
				"include","moduleLocation");
		} else
            try {
    			reader.switchSource(this,href,new RootModuleMergeState());
            } catch( AbortException e ) {}
		// recover by ignoring this include element
	}
}
