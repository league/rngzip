package org.kohsuke.bali.automaton;

import org.kohsuke.bali.automaton.builder.NameClassEncoder;

import com.sun.msv.grammar.NameClass;

/**
 * Signature of a name class.
 * 
 * Checking whether a (uri,local) pair is accepted to a name class
 * or not is a costly operation.
 * 
 * Bali avoids the use of name classes at the runtime by replacing
 * <code>NameClass.accepts(name)</code> by the following bit test:
 * <code>(nameCode&amp;mask)==test</code>
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class NameSignature {
    public NameSignature( NameClass nc, int mask, int test, NameClassEncoder encoder ) {
        this.nameClass = nc;
        this.mask=mask; 
        this.test=test;
        this.owner = encoder;
    }
    
    /** NameClass object from which this signature is derived. */
    public final NameClass nameClass;
    
    /** Bit mask to be applied. */
    public final int mask;
    
    /** Test bits to be tested. */
    public final int test;
    
    /**
     * NameClassEncoder object that created this signature.
     * Used just for debugging purpose.
     */
    private final NameClassEncoder owner;
    
    public boolean accepts( int nameCode ) {
        return (nameCode&mask)==test;
    }
    
    public String toString() {
        String s="";
        for( int i=0; i<owner.getUsedBits(); i++ )
            if( (mask&(1<<i))!=0 ) {
                if( (test&(1<<i))!=0 )  s='1'+s;
                else                    s='0'+s;
            } else
                s='-'+s;
            
        return s+" : "+nameClass;
    }
}
