/*
 * @(#)$Id: ListState.java,v 1.11 2002/06/24 19:57:58 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.datatype.xsd;

import com.sun.msv.reader.GrammarReader;
import com.sun.msv.reader.IgnoreState;
import com.sun.msv.reader.State;
import com.sun.msv.datatype.xsd.XSDatatype;
import com.sun.msv.datatype.xsd.DatatypeFactory;
import com.sun.msv.util.StartTagInfo;
import org.relaxng.datatype.DatatypeException;

/**
 * state that parses &lt;list&gt; element of XSD.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ListState extends TypeWithOneChildState
{
    protected final String newTypeUri;
    protected final String newTypeName;
    
    protected ListState( String newTypeUri, String newTypeName ) {
        this.newTypeUri  = newTypeUri;
        this.newTypeName = newTypeName;
    }
	
	protected XSDatatypeExp annealType( final XSDatatypeExp itemType ) throws DatatypeException {
        return XSDatatypeExp.makeList( newTypeUri, newTypeName, itemType, reader );
    }
	
	protected void startSelf() {
		super.startSelf();
		
		// if itemType attribute is used, use it.
		String itemType = startTag.getAttribute("itemType");
		if(itemType!=null)
            onEndChild( ((XSDatatypeResolver)reader).resolveXSDatatype(itemType) );
	}

	protected State createChildState( StartTagInfo tag ) {
		// accepts elements from the same namespace only.
		if( !startTag.namespaceURI.equals(tag.namespaceURI) )	return null;
		
		if( tag.localName.equals("annotation") )	return new IgnoreState();
		if( tag.localName.equals("simpleType") )	return new SimpleTypeState();
		
		return null;	// unrecognized
	}
}
