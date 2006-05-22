/*
 * @(#)$Id: NameClassAndExpression.java,v 1.1 2001/05/04 01:49:01 Bear Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.grammar;

/**
 * common interface of AttributeExp and ElementExp.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public interface NameClassAndExpression {
	NameClass getNameClass();
	Expression getContentModel();
}
