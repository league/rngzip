/*
 * @(#)$Id: TREXFactoryImpl.java,v 1.11 2001/11/01 00:23:09 kk122374 Exp $
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
import com.sun.msv.reader.GrammarReaderController;
import com.sun.msv.reader.trex.classic.TREXGrammarReader;
import org.xml.sax.InputSource;

/**
 * VerifierFactory implementation for TREX.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class TREXFactoryImpl extends FactoryImpl
{
	protected Grammar parse( InputSource is, GrammarReaderController controller ) {
		return TREXGrammarReader.parse(is,factory,controller);
	}
}
