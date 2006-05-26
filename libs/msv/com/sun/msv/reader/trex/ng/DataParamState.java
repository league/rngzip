/*
 * @(#)$Id: DataParamState.java,v 1.2 2001/06/19 22:34:29 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.trex.ng;

import com.sun.msv.reader.ChildlessState;
import org.relaxng.datatype.DatatypeException;

/**
 * parses &lt;param&gt; element inside &lt;data&gt; element.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class DataParamState extends ChildlessState {
	
	protected final StringBuffer text = new StringBuffer();
	
	public void characters( char[] buf, int from, int len ) {
		text.append(buf,from,len);
	}
	public void ignorableWhitespace( char[] buf, int from, int len ) {
		text.append(buf,from,len);
	}
	protected void endSelf() {
		final String facet = startTag.getAttribute("name");
		if(facet==null)
			reader.reportError( reader.ERR_MISSING_ATTRIBUTE, startTag.qName, "name" );
		else {
			try {
				((DataState)parentState).typeBuilder.addParameter(
					facet, text.toString(), reader );
			} catch( DatatypeException dte ) {
				reader.reportError( RELAXNGReader.ERR_BAD_FACET, facet, dte.getMessage() );
			}
		}		
		super.endSelf();
	}
}
