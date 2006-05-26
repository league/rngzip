/*
 * @(#)$Id: SchemaState.java,v 1.6 2001/05/01 18:13:20 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.xmlschema;

import com.sun.msv.grammar.xmlschema.XMLSchemaSchema;

/**
 * used to parse &lt;schema&gt; element.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class SchemaState extends SchemaIncludedState {

	protected SchemaState( String expectedTargetNamespace ) {
		super(expectedTargetNamespace);
	}
	
	private XMLSchemaSchema old;
	
	protected void onTargetNamespaceResolved( String targetNs ) {
		super.onTargetNamespaceResolved(targetNs);
		
		final XMLSchemaReader reader = (XMLSchemaReader)this.reader;
		
		// sets new XMLSchemaGrammar object.
		old = reader.currentSchema;
		reader.currentSchema = reader.getOrCreateSchema(targetNs);
		
		if( reader.isSchemaDefined(reader.currentSchema) )  {
			reader.reportError( reader.ERR_DUPLICATE_SCHEMA_DEFINITION, targetNs );
			// recover by providing dummy grammar object.
			// this object is not registered to the map,
			// so it cannot be referenced.
			reader.currentSchema = new XMLSchemaSchema(targetNs,reader.grammar);
		}
		
		reader.markSchemaAsDefined(reader.currentSchema);
	}
	
	protected void endSelf() {
		final XMLSchemaReader reader = (XMLSchemaReader)this.reader;
		reader.currentSchema = old;
		super.endSelf();
	}
}
