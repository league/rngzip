/*
 * @(#)$Id: IgnoreController.java,v 1.6 2001/05/01 18:13:17 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.util;

import com.sun.msv.reader.GrammarReaderController;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;

/**
 * Default implementation of GrammarReaderController.
 * 
 * This class ignores every errors and warnings.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class IgnoreController implements GrammarReaderController
{
	public void warning( Locator[] locs, String errorMessage ) {}
	public void error( Locator[] locs, String errorMessage, Exception nestedException ) {}
	public InputSource resolveEntity( String p, String s ) { return null; }
}
