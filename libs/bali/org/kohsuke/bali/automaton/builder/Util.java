package org.kohsuke.bali.automaton.builder;

import org.kohsuke.bali.datatype.DatatypeImpl;
import org.relaxng.datatype.Datatype;

import com.sun.msv.datatype.xsd.XSDatatype;
import com.sun.msv.grammar.AttributeExp;
import com.sun.msv.grammar.ChoiceExp;
import com.sun.msv.grammar.DataExp;
import com.sun.msv.grammar.Expression;
import com.thaiopensource.datatype.Datatype2;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Util {
    public static boolean isIgnorableOptionalAttribute( ChoiceExp exp ) {
        return checkIgnororableOptionalAttribute(exp.exp1,exp.exp2)
            || checkIgnororableOptionalAttribute(exp.exp2,exp.exp1);
    }
    
    /**
     * Returns true if e1 is &lt;empty/> and e2 is an attribute whose
     * body is &lt;text/>
     */
    private static boolean checkIgnororableOptionalAttribute( Expression e1, Expression e2 ) {
        return e1 == Expression.epsilon
            && e2 instanceof AttributeExp
            && isIgnorableText(((AttributeExp)e2).exp);
    }
    
    /**
     * Returns true if the given expression can be satisfied by
     * any string.
     */
    private static boolean isIgnorableText( Expression e ) {
        if( e== Expression.anyString )     return true;
        if( e instanceof DataExp ) {
            DataExp d = (DataExp)e;
            if( d.except!=Expression.nullSet )
                return false;
            
            Datatype dt = ((DatatypeImpl)d.dt).realDatatype;
            
            if( dt instanceof Datatype2 )
                return ((Datatype2)dt).alwaysValid();
            
            return false;
        }
        
        return false;
    }
}
