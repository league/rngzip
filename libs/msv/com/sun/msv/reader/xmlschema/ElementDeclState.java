/*
 * @(#)$Id: ElementDeclState.java,v 1.31 2002/09/13 15:08:40 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.xmlschema;

import com.sun.msv.datatype.xsd.XSDatatype;
import com.sun.msv.datatype.xsd.BooleanType;
import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.AttributeExp;
import com.sun.msv.grammar.ReferenceExp;
import com.sun.msv.grammar.SimpleNameClass;
import com.sun.msv.grammar.trex.ElementPattern;
import com.sun.msv.grammar.xmlschema.ElementDeclExp;
import com.sun.msv.grammar.xmlschema.ComplexTypeExp;
import com.sun.msv.grammar.xmlschema.XMLSchemaSchema;
import com.sun.msv.grammar.xmlschema.XMLSchemaTypeExp;
import com.sun.msv.grammar.xmlschema.IdentityConstraint;
import com.sun.msv.util.StartTagInfo;
import com.sun.msv.util.StringPair;
import com.sun.msv.reader.GrammarReader;
import com.sun.msv.reader.State;
import com.sun.msv.reader.IgnoreState;
import com.sun.msv.reader.ExpressionWithChildState;
import org.xml.sax.Locator;
import org.relaxng.datatype.Datatype;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * used to parse &lt;element &gt; element without ref attribute.
 * 
 * this state uses ExpressionWithChildState to collect content model
 * of this element declaration.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class ElementDeclState extends ExpressionWithChildState {

	protected State createChildState( StartTagInfo tag ) {
		final XMLSchemaReader reader = (XMLSchemaReader)this.reader;
		
		// type declaration is allowed only when we haven't seen type definition.
		if( super.exp==null ) {
			if( tag.localName.equals("simpleType") )	return reader.sfactory.simpleType(this,tag);
			if( tag.localName.equals("complexType") )	return reader.sfactory.complexTypeDecl(this,tag);
		}
		// unique/key/keyref are ignored.
		if( tag.localName.equals("unique") )	return reader.sfactory.unique(this,tag);
		if( tag.localName.equals("key") )		return reader.sfactory.key(this,tag);
		if( tag.localName.equals("keyref") )	return reader.sfactory.keyref(this,tag);
		
		return null;
	}

	protected Expression initialExpression() {
		// if <element> element has type attribute, then
		// it shall be used as content type.
		String typeQName = startTag.getAttribute("type");
		if( typeQName==null )	return null;

        return resolveTypeRef(typeQName);
    }
    
    /**
     * If this element declaration has @type, then this method
     * is called to resolve it.
     * 
     * Since the type refered to may not be processed yet,
     * a late binding is needed here.
     */
    protected Expression resolveTypeRef( final String typeQName ) {
		final XMLSchemaReader reader = (XMLSchemaReader)this.reader;
		
		// TODO: shall I memorize this as a backward reference?
		// reader.backwardReference.memorizeLink(???);
		
		// symbol may not be defined at this moment.
		// so just return an empty ReferenceExp and back-patch the actual definition later.
		final ReferenceExp ref = new ReferenceExp("elementType("+typeQName+")");
		
		final String[] s = reader.splitQName(typeQName);
		if(s==null) {
			reader.reportError( reader.ERR_UNDECLARED_PREFIX, typeQName );
			ref.exp = Expression.nullSet;	// recover by setting a dummy definition.
			return ref;
		}
		
		reader.addBackPatchJob( new GrammarReader.BackPatch(){
			public State getOwnerState() { return ElementDeclState.this; }
			public void patch() {
				
				Expression e=null;
				
				if( reader.isSchemaNamespace(s[0]) )
					// datatypes of XML Schema part 2
                    e = reader.resolveBuiltinSimpleType(s[1]);
				
				if(e==null) {
					XMLSchemaSchema g = reader.getOrCreateSchema(s[0]/*uri*/);
					e = g.simpleTypes.get(s[1]/*local name*/);
					if(e==null)	e = g.complexTypes.get(s[1]);
					if(e==null ) {
						// both simpleType and complexType are undefined.
						reader.reportError( reader.ERR_UNDEFINED_ELEMENTTYPE, typeQName );
						e = Expression.nullSet;	// recover by dummy definition.
					}
				}
				ref.exp = e;
			}
		});
		
		return ref;
	}

	protected Expression castExpression( Expression halfCastedExpression, Expression newChildExpression ) {
		
		if( halfCastedExpression!=null )
			// assertion failed:
			// createChildState shouldn't allow parsing child <simpleType>
			// if one is already present.
			throw new Error();
		
		return newChildExpression;
	}
																												   
	protected Expression defaultExpression() {
		if( startTag.containsAttribute("substitutionGroup") )
			reader.reportError( XMLSchemaReader.ERR_UNIMPLEMENTED_FEATURE,
				"omitting type attribute in <element> element with substitutionGroup attribute");
			// recover by assuming ur-type.
			
		// if no content model is given, then this element type is ur-type.
		// TODO: confirm it.
		final XMLSchemaReader reader = (XMLSchemaReader)this.reader;
		reader.reportWarning( reader.WRN_IMPLICIT_URTYPE_FOR_ELEMENT, null );
		return reader.complexUrType;
	}
	
	protected Expression annealExpression(Expression contentType) {
		final XMLSchemaReader reader = (XMLSchemaReader)this.reader;
		
		String name = startTag.getAttribute("name");
		if( name==null ) {
			reader.reportError( reader.ERR_MISSING_ATTRIBUTE, "element", "name" );
			// recover by abandoning this element.
			return Expression.nullSet;
		}
		
		String targetNamespace;
		if( isGlobal() )
			// TODO: form attribute is prohibited at toplevel.
			targetNamespace = reader.currentSchema.targetNamespace;
		else
			// in local attribute declaration,
			// targetNamespace is affected by @form and schema's @attributeFormDefault.
			targetNamespace = ((XMLSchemaReader)reader).resolveNamespaceOfElementDecl(
				startTag.getAttribute("form") );
		
		
		// TODO: better way to handle the "fixed" attribtue.
		String fixed = startTag.getAttribute("fixed");
		if( fixed!=null )
			// TODO: is this 'fixed' value should be added through enumeration facet?
			// TODO: check if content model is a simpleType.
			contentType = reader.pool.createValue(
				com.sun.msv.datatype.xsd.TokenType.theInstance,
				new StringPair("","token"), fixed ); // emulate RELAX NG built-in token type
		
		
		ElementDeclExp decl;
		if( isGlobal() ) {
			decl = reader.currentSchema.elementDecls.getOrCreate(name);
			if( decl.getElementExp()!=null )
				reader.reportError( 
					new Locator[]{this.location,reader.getDeclaredLocationOf(decl)},
					reader.ERR_DUPLICATE_ELEMENT_DEFINITION,
					new Object[]{name} );
			
		} else {
			// create a local object.
			decl = new ElementDeclExp(reader.currentSchema,null);
		}
		
		reader.setDeclaredLocationOf(decl);

		ElementDeclExp.XSElementExp exp = decl.new XSElementExp(
			new SimpleNameClass(targetNamespace,name), contentType );
		
		// set the body.
		decl.setElementExp(exp);

		// set identity constraints
		exp.identityConstraints.addAll(idcs);
		
		// process the nillable attribute.
		boolean isNillable = false;
		String nillable = startTag.getAttribute("nillable");
		if( nillable!=null )
            decl.isNillable = nillable.equals("true") || nillable.equals("1");

		// process the "abstract" attribute.
		String abstract_ = startTag.getAttribute("abstract");
		decl.setAbstract( "true".equals(abstract_)||"1".equals(abstract_) );
		if( abstract_!=null && !BooleanType.theInstance.isValid(abstract_,null) )
			// recover by assuming false.
			reader.reportError( XMLSchemaReader.ERR_BAD_ATTRIBUTE_VALUE, "abstract", abstract_ );
		
		// TODO: abstract is prohibited for the local element.
		
		// TODO: substitutionGroup is also prohibited for the local element.	
		String substitutionGroupQName = startTag.getAttribute("substitutionGroup");
		if( substitutionGroupQName!=null ) {
			String[] r = reader.splitQName(substitutionGroupQName);
			if(r==null) {
				reader.reportError( reader.ERR_UNDECLARED_PREFIX, substitutionGroupQName );
				// recover by ignoring substitutionGroup.
			} else {
				// register this declaration to the head elementDecl.
				ElementDeclExp head = reader.getOrCreateSchema(r[0]/*uri*/).
					elementDecls.getOrCreate(r[1]/*local name*/);
				
				decl.substitutionAffiliation = head;
				
				// before adding this "decl" to "head.substitutions",
				// we need to check the block attribute of various related components.
				// Therefore, this process is done at the wrapUp method of XMLSchemaReader.
			}
		}
		
		String block = startTag.getAttribute("block");
		if(block==null)		block = reader.blockDefault;
		if(block!=null) {
			if( block.indexOf("#all")>=0 )
				decl.block |= decl.RESTRICTION|decl.EXTENSION|decl.SUBSTITUTION;
			if( block.indexOf("extension")>=0 )
				decl.block |= decl.EXTENSION;
			if( block.indexOf("restriction")>=0 )
				decl.block |= decl.RESTRICTION;
			if( block.indexOf("substitution")>=0 )
				decl.block |= decl.SUBSTITUTION;
		}
		
		String finalValue = startTag.getAttribute("final");
		if(finalValue==null)	finalValue = reader.finalDefault;
		if(finalValue!=null) {
			if( finalValue.indexOf("#all")>=0 )
				decl.finalValue |= decl.RESTRICTION|decl.EXTENSION|decl.SUBSTITUTION;
			if( finalValue.indexOf("extension")>=0 )
				decl.finalValue |= decl.EXTENSION;
			if( finalValue.indexOf("restriction")>=0 )
				decl.finalValue |= decl.RESTRICTION;
		}
		
		// minOccurs/maxOccurs is processed through interception
        // call the hook to let derived classes modify the content model
		return annealDeclaration(decl);
	}
    
    /**
     * This method is called after this class finishes augmenting
     * ElementDeclExp. Derived classes can override this method
     * and modify an ElementDeclExp further.
     */
    protected Expression annealDeclaration( ElementDeclExp exp ) {
        return exp;
    }

    /**
     * Returns true if this element declaration is a global element declaration.
     */
	public boolean isGlobal() {
		return parentState instanceof GlobalDeclState;
	}

	
	/** identity constraints found in this element. */
	protected final Vector idcs = new Vector();
		
	/** this method is called when an identity constraint declaration is found.
	 */
	protected void onIdentityConstraint( IdentityConstraint idc ) {
		idcs.add(idc);
	}
}
