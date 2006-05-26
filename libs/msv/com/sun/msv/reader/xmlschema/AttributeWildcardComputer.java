/*
 * @(#)$Id: AttributeWildcardComputer.java,v 1.3 2002/09/27 04:14:32 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.reader.xmlschema;

import com.sun.msv.grammar.*;
import com.sun.msv.grammar.xmlschema.*;
import com.sun.msv.grammar.util.ExpressionWalker;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;

/**
 * Processes the attribtue wildcard according to the spec.
 * 
 * <p>
 * Since the definition of the attribute wildcard is very adhoc,
 * it cannot be naturally caputred by our AGM.
 * 
 * <p>
 * Therefore, when we parse a schema, we just parse &lt;anyAttribute> directly.
 * After all components are loaded, arcane computation is done to correctly
 * compute the attribute wildcard.
 * 
 * <p>
 * Attribute wildcard will be ultimately converted into an expression, and that
 * will be attached to the {@link ComplexTypeExp#attWildcard}.
 * 
 * <p>
 * This class also computes the attribute propagation that happens
 * only when a complex type is derived by restriction.
 * 
 * Consider the following fragment:
 * 
 * <pre><xmp>
 * <complexType name="base">
 *   <attribute name="abc" ... />
 * </complexType>
 * 
 * <complexType name="derived">
 *   <complexContent>
 *     <restriction base="base"/>
 *   </complexContent>
 * </complexType>
 * </xmp></pre>
 * 
 * <p>
 * According to the spec, the derived type will have the 'abc' attribute.
 * By "propagation", we mean this behavior.
 * 
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class AttributeWildcardComputer extends ExpressionWalker {

    public static void compute( XMLSchemaReader reader, Expression topLevel ) {
        new AttributeWildcardComputer(reader).compute(topLevel);
    }
    
    private void compute( Expression topLevel ) {
        topLevel.visit(this);
        while(!unprocessedElementExps.isEmpty())
            ((ElementExp)unprocessedElementExps.pop()).contentModel.visit(this);
    }
	
    
	protected AttributeWildcardComputer( XMLSchemaReader _reader ) {
		this.reader = _reader;
	}
	
	private final XMLSchemaReader reader;
	
	/**
	 * Visited ElementExps and ReferenceExps to prevent infinite recursion.
	 */
	private final Set visitedExps = new HashSet();
    
    private final Stack unprocessedElementExps = new Stack();
	
	/**
	 * Used to collect AttributeWildcards of children.
	 */
	private Set wildcards = null;
    
	
	public void onElement( ElementExp exp ) {
		if( !visitedExps.add(exp) )
			return;		// this element has already been processed
        unprocessedElementExps.add(exp);
	}
	
	public void onRef( ReferenceExp exp ) {
		if( visitedExps.add(exp) ) {
			if( exp instanceof AttributeGroupExp ) {
				AttributeGroupExp aexp = (AttributeGroupExp)exp;
				
				final Set o = wildcards;
				{
					// process children and collect their wildcards.
					wildcards = new HashSet();
					exp.exp.visit(this);
					// compute the attribute wildcard
					aexp.wildcard = calcCompleteWildcard( aexp.wildcard, wildcards );
				}
				wildcards = o;
			}
			else
			if( exp instanceof ComplexTypeExp ) {
				ComplexTypeExp cexp = (ComplexTypeExp)exp;
				
				final Set o = wildcards;
				{
					// process children and collect their wildcards.
					wildcards = new HashSet();
					exp.exp.visit(this);
					// compute the attribute wildcard
					cexp.wildcard = calcCompleteWildcard( cexp.wildcard, wildcards );
					
//					if(cexp.wildcard==null)
//						System.out.println("complete wildcard is: none");
//					else
//						System.out.println("complete wildcard is: "+cexp.wildcard.getName());
					
					// if the base type is a complex type and the extension is chosen,
					// then we need one last step. Sigh.
					
					if(cexp.complexBaseType!=null) {
//						System.out.println("check the base type");
						
						// process the base type first.
						cexp.complexBaseType.visit(this);
						if(cexp.derivationMethod==cexp.EXTENSION)
							cexp.wildcard = calcComplexTypeWildcard(
								cexp.wildcard,
								cexp.complexBaseType.wildcard );
						
						propagateAttributes(cexp);
					}
					
					// create the expression for this complex type.
					if( cexp.wildcard!=null )
						cexp.attWildcard.exp = cexp.wildcard.createExpression(reader.grammar);
				}
				wildcards = o;
			} else
				// otherwise process it normally.
				super.onRef(exp);
		}
		
		if( wildcards!=null ) {
			// add the complete att wildcard of this component.
			if( exp instanceof AttWildcardExp ) {
				AttributeWildcard w = ((AttWildcardExp)exp).getAttributeWildcard();
				if(w!=null)	wildcards.add(w);
			}
		}
	}
	
	/**
	 * Computes the "complete attribute wildcard"
	 */
	private AttributeWildcard calcCompleteWildcard( AttributeWildcard local, Set s ) {
		final AttributeWildcard[] children =
			(AttributeWildcard[])s.toArray(new AttributeWildcard[s.size()]);
		
		// 1st step is to compute the complete wildcard.
		if( children.length==0 )
			return local;
		
		// assert(children.length>0)
			
		// compute the intersection of wildcard.
		NameClass target = children[0].getName();
		for( int i=1; i<children.length; i++ )
			target = NameClass.intersection(target,children[i].getName());
			
		if( local!=null )
			return new AttributeWildcard(
				NameClass.intersection(local.getName(),target),
				local.getProcessMode() );
		else
			return new AttributeWildcard(
				target, children[0].getProcessMode() );
	}

	private AttributeWildcard calcComplexTypeWildcard(
		AttributeWildcard complete, AttributeWildcard base ) {

		if(base!=null) {
			if(complete==null)
				return base;
			else
				return new AttributeWildcard(
					NameClass.union( complete.getName(), base.getName() ),
					complete.getProcessMode() );
		} else {
			// the spec does not have a description for this case.
			// this is my guess.
			return complete;
		}
	}
	
	/**
	 * Computes the propagated attributes.
	 */
	private void propagateAttributes( final ComplexTypeExp cexp ) {
		// propagation will be done only if this type is derived from
		// another complex type by restriction.
		if(cexp.derivationMethod!=cexp.RESTRICTION || cexp.complexBaseType==null)
			return;

		// strangely, this computation does not apply if the base type is
		// complex ur-type.
		if( cexp.complexBaseType==reader.complexUrType )
			return;
		
		final Set explicitAtts = new HashSet();
		
		// visit the derived type and enumerate explicitly declared attributes in it.
		cexp.body.visit( new ExpressionWalker() {
			// stop if we hit an ElementExp.
			public void onElement( ElementExp exp ) {}
			public void onAttribute( AttributeExp exp ) {
				if(!(exp.nameClass instanceof SimpleNameClass))
					// attribute uses must have a simple name.
					throw new Error(exp.nameClass.toString());
				
				explicitAtts.add( ((SimpleNameClass)exp.nameClass).toStringPair() );
			}
		});
		
		
		// visit the base type and enumerate all attributes in it.
		cexp.complexBaseType.body.visit( new ExpressionWalker() {
			
			private boolean isOptional = false;
			
			public void onChoice( ChoiceExp exp ) {
				boolean b = isOptional;
				isOptional = true;
				super.onChoice(exp);
				isOptional = b;
			}
			
			// stop if we hit an ElementExp.
			public void onElement( ElementExp exp ) {}
			public void onAttribute( AttributeExp exp ) {
				// found an attribute
				if(!(exp.nameClass instanceof SimpleNameClass))
					throw new Error();	// attribute uses must have a simple name.
				
				SimpleNameClass snc = (SimpleNameClass)exp.nameClass;
				
				// see if the dervied type has a definition that
				// overrides this attribute.
				if( !explicitAtts.contains(snc.toStringPair()) ) {
					// this attribute is not defined. copy it.
					cexp.body.exp = reader.pool.createSequence(
						cexp.body.exp,
						isOptional?reader.pool.createOptional(exp):exp );
				}
			}
		});
	}
}
