package org.kohsuke.bali.automaton;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ListAlphabet extends Alphabet {
    private ListAlphabet() {}
    
    public static final ListAlphabet theInstance = new ListAlphabet();
    
    public Object accept( AlphabetVisitor visitor ) {
        return visitor.list(this);
    }

    public boolean isPersistent() { return true; }
}
