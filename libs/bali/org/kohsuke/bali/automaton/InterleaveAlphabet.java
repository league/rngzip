package org.kohsuke.bali.automaton;

import com.sun.msv.grammar.NameClass;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class InterleaveAlphabet extends Alphabet {
    
    /** State where two sub-automata joins. */
    public final State join;
    
    /** Name class of all attributes in the left branch. */
    public final NameClass leftAttributes;
    /** Name class of all attributes in the right branch. */
    public final NameClass rightAttributes;
    
    /**
     * Flag that indicates the branch that can perform text transitions.
     * True if the left branch is the one. False if the right branch is
     * the one.
     * 
     * If none of both can perform text transitions, then it doesn't
     * matter what this flag is.
     */
    public final boolean textToLeft;
    
    public InterleaveAlphabet( State s, NameClass l, NameClass r, boolean _textToLeft ) {
        this.join = s;
        this.leftAttributes = l;
        this.rightAttributes = r;
        this.textToLeft = _textToLeft;
    }
    
    public Object accept( AlphabetVisitor visitor ) {
        return visitor.interleave(this);
    }

    public boolean isPersistent() { return false; }
}
