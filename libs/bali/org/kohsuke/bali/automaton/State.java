package org.kohsuke.bali.automaton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.msv.grammar.Expression;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class State {

    /** All transitions from this state. */
    private Set transitions = new HashSet();
    
    /** Unique ID of this state. */
    public final int id;
    
    /** True if this state is one of the final states. */
    public final boolean isFinal;
    
    /**
     * A state is persistent if it has a "real" transition
     * (a transition by ElementAlphabet or Data/ValueAlphabet),
     * or when there's no transition from the state at all.
     */
    private boolean isPersistent;
    public boolean isPersistent() { return isPersistent || transitions.isEmpty(); }
    
    /**
     * The state whose transitions should be inherited by
     * this state.
     * 
     * Another way to look at this state is that there's
     * a epsilon transition to this state.
     * 
     * This variable can be null.
     */
    public final State nextState;
    
    /**
     * Expression that represents this state.
     */
    public final Expression exp;
    
    
    /** Create a new State through TreeAutomaton. */
    protected State( Expression _exp, boolean _isFinal, int _id, State _nextState ) {
        this.exp = _exp;
        this.isFinal = _isFinal;
        this.id = _id;
        this.nextState = _nextState;
    }
    
    public void addTransition( Alphabet alpha, State left, State right ) {
        transitions.add( new Transition(alpha,left,right) );
        if( alpha.isPersistent() )  isPersistent = true;
    }
    
    public void addTransition( DataAlphabet alpha, State right ) {
        transitions.add( new Transition(alpha,null,right) );
        if( alpha.isPersistent() )  isPersistent = true;
    }
    
    /**
     * Gets the transitions from this state without including epsilon closure.
     */
    public Transition[] getDeclaredTransitions() {
        return (Transition[]) transitions.toArray(new Transition[transitions.size()]);
    }
    
    public Transition[] getTransitions() {
        if(nextState==null)
            return (Transition[]) transitions.toArray(new Transition[transitions.size()]);
        else {
            ArrayList list = new ArrayList();
            for( State s=this; s!=null; s=s.nextState )
                list.addAll(s.transitions);
            return (Transition[]) list.toArray(new Transition[list.size()]);
        }
    }

    public int countTransitions() {
        return transitions.size();
    }
    
    
    public static final int TEXT_WHITESPACE_ONLY = 0;
    public static final int TEXT_IGNORABLE = 1;
    public static final int TEXT_SENSITIVE = 2;
    
    /**
     * Returns true if text is ignorable on this state.
     */
    public int getTextSensitivity() {
        boolean loopbackTransition=false;
        boolean otherTextTransition=false;
        
        Transition[] trans = getTransitions();
        for( int i=0; i<trans.length; i++ ) {
            Transition t = trans[i];
            
            if( t.alphabet instanceof ValueAlphabet ) {
                otherTextTransition = true;
            }
            if( t.alphabet instanceof DataAlphabet ) {
                DataAlphabet da = (DataAlphabet)t.alphabet;
                if( da.isAlwaysValid() && t.left.isNullSet() && t.right==this )
                    loopbackTransition = true;
                else
                    otherTextTransition = true;
            }
            if( t.alphabet instanceof ListAlphabet )
                otherTextTransition = true;
        }
        
        if( loopbackTransition && !otherTextTransition )
            return TEXT_IGNORABLE;
        
        if( otherTextTransition )
            return TEXT_SENSITIVE;
        
        return TEXT_WHITESPACE_ONLY;
    }

    
    /** Returns true if this state represents the terminal epsilon state. */
    public boolean isEpsilon() {
        return isFinal && transitions.isEmpty();
    }
    
    /** Returns true if this state represents the terminal empty set state. */
    public boolean isNullSet() {
        return !isFinal && transitions.isEmpty();
    }
    
    public String toString() { return "s"+id+(isPersistent?"":"_"); }
}
