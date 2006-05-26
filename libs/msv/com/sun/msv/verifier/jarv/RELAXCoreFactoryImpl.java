/*
 * @(#)$Id: RELAXCoreFactoryImpl.java,v 1.7 2001/11/01 00:23:09 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.verifier.jarv;

import org.iso_relax.verifier.*;
import com.sun.msv.grammar.Grammar;
import com.sun.msv.grammar.ExpressionPool;
import com.sun.msv.reader.GrammarReaderController;
import com.sun.msv.reader.relax.core.RELAXCoreReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * VerifierFactory implementation of RELAX Core.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class RELAXCoreFactoryImpl extends FactoryImpl
{
	protected Grammar parse( InputSource is, GrammarReaderController controller ) {
		return RELAXCoreReader.parse(is,factory,controller,new ExpressionPool());
	}
}
