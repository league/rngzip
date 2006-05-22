package org.kohsuke.bali.automaton.builder;

import java.util.HashSet;
import java.util.Set;

import org.kohsuke.bali.automaton.NameSignature;

import com.sun.msv.grammar.AttributeExp;
import com.sun.msv.grammar.ElementExp;
import com.sun.msv.grammar.Expression;
import com.sun.msv.grammar.ListExp;
import com.sun.msv.grammar.ReferenceExp;
import com.sun.msv.grammar.util.ExpressionWalker;

/**
 * Collects all NameSignatures from attributes.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AttNameSigCollector extends ExpressionWalker {
    
    public static Set collect( Expression exp, NameClassEncoder enc ) {
        AttNameSigCollector ansc = new AttNameSigCollector(enc);
        exp.visit(ansc);
        return ansc.nameSignatures;
    }
    
    private AttNameSigCollector( NameClassEncoder enc ) {
        this.encoder = enc;
    }
    
    private final Set nameSignatures = new HashSet();
    private final NameClassEncoder encoder;
        
    /**
     * @see com.sun.msv.grammar.ExpressionVisitorVoid#onAttribute(AttributeExp)
     */
    public void onAttribute(AttributeExp exp) {
        nameSignatures.add( encoder.getSignature(exp.nameClass) );
    }

    public void onElement(ElementExp exp) {}
    
    // no need to visit inside. It can't contain attributes
    public void onList(ListExp exp) {}
}
