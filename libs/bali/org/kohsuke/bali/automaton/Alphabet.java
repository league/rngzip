package org.kohsuke.bali.automaton;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class Alphabet {
    public abstract Object accept( AlphabetVisitor visitor );
    
    /**
     * An alphabet is said to be persistent if it is a part of
     * ordered items in XML (such as texts/elements.)
     */
    public abstract boolean isPersistent();
}
