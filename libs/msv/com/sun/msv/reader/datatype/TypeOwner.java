/*
 * @(#)$Id: TypeOwner.java,v 1.7 2001/11/21 22:15:18 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.datatype;

import com.sun.msv.grammar.Expression;

/**
 * State can implement this method to be notified by DataType vocabulary
 * about the result of parsing.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public interface TypeOwner {
	void onEndChildType( Expression datatype, String typeName );
}
