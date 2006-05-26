/*
 * @(#)$Id: DataTypeWithFacet.java,v 1.28 2002/11/07 16:50:03 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd;

import org.relaxng.datatype.*;
import com.sun.msv.datatype.SerializationContext;

/**
 * Base implementation of facet-restricted datatype
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public abstract class DataTypeWithFacet extends XSDatatypeImpl
{
	/** immediate base type, which may be a concrete type or DataTypeWithFacet */
	public final XSDatatypeImpl baseType;
	final public XSDatatype getBaseType() { return baseType; }
		
	
	/** base concrete type */
	protected final ConcreteType concreteType;
	
	/** name of this facet */
	public final String facetName;
	
	/** a flag that indicates this type has value-constraint facet.
	 * 
	 * this value is used to cache this flag.
	 */
	private final boolean needValueCheckFlag;
	
	/** constructor for facets other than WhiteSpaceFacet */
	DataTypeWithFacet( String nsUri, String typeName, XSDatatypeImpl baseType, String facetName, TypeIncubator facets )
		throws DatatypeException {
		this( nsUri, typeName, baseType, facetName, facets, baseType.whiteSpace );
	}
	
	/** constructor for WhiteSpaceFacet */
	DataTypeWithFacet( String nsUri, String typeName, XSDatatypeImpl baseType, String facetName, TypeIncubator facets, WhiteSpaceProcessor whiteSpace )
		throws DatatypeException {
		super(nsUri,typeName, whiteSpace);
		this.baseType = baseType;
		this.facetName = facetName;
		this.concreteType = baseType.getConcreteType();
		
		needValueCheckFlag = baseType.needValueCheck();
		
		int r = baseType.isFacetApplicable(facetName);
		switch(r)
		{
		case APPLICABLE:	return;	// this facet is applicable to this type. no problem.
		case NOT_ALLOWED:
			throw new DatatypeException( localize(ERR_NOT_APPLICABLE_FACET, facetName) );
		}
	}
	
	public boolean isContextDependent() {
		return concreteType.isContextDependent();
	}
	
	public int getIdType() {
		return concreteType.getIdType();
	}
	
	public final String displayName() {
        if( getName()!=null )   return getName();
		else                      return concreteType.getName()+"-derived";
	}
	
	public final int isFacetApplicable( String facetName ) {
		return baseType.isFacetApplicable(facetName);
	}
	
	protected boolean needValueCheck() { return needValueCheckFlag; }
	
	final public DataTypeWithFacet getFacetObject( String facetName ) {
		if(this.facetName.equals(facetName))
			return this;
		else
			return baseType.getFacetObject(facetName);
	}
	
	final public ConcreteType getConcreteType() {
		return concreteType;
	}
	
	final public int getVariety() {
		return concreteType.getVariety();
	}
	
	final public boolean isFinal( int derivationType ) {
		return baseType.isFinal(derivationType);
	}
	
	final public String convertToLexicalValue( Object o, SerializationContext context ) {
		return concreteType.convertToLexicalValue(o,context);
	}
	final public Class getJavaObjectType() {
		return concreteType.getJavaObjectType();
	}
// DatabindableDatatype implementation
	final public Object _createJavaObject( String literal, ValidationContext context ) {
		// TODO: this can be more efficient
		if(isValid(literal,context))
			return baseType.createJavaObject(literal,context);
		else
			return null;
	}
	public String serializeJavaObject( Object value, SerializationContext context ) {
		return baseType.serializeJavaObject( value, context );
	}
	
	final protected void _checkValid(String content, ValidationContext context ) throws DatatypeException {
		// let the base type complain first.
		baseType._checkValid(content,context);
		
		// then see if the facet is satisfied.
		diagnoseByFacet(content,context);
	}
	
	protected abstract void diagnoseByFacet(String content, ValidationContext context)
		throws DatatypeException;

}
