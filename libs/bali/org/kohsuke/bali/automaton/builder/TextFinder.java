package org.kohsuke.bali.automaton.builder;

import com.sun.msv.grammar.*;
import com.sun.msv.grammar.util.ExpressionFinder;

/**
 * Returns true if an expression contains text transitions.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class TextFinder extends ExpressionFinder {
    private TextFinder() {}
    private static final TextFinder theInstance = new TextFinder();
    
    public static boolean find( Expression e ) {
        return e.visit(theInstance);
    }
    
    public boolean onData(DataExp exp)      { return true; }
    public boolean onList(ListExp exp)      { return true; }
    public boolean onValue(ValueExp exp)    { return true; }
    public boolean onAnyString()            { return true; }
    public boolean onMixed(MixedExp exp)    { return true; }
    public boolean onAttribute(AttributeExp exp){ return false; }
    public boolean onElement(ElementExp exp)    { return false; }
}
