package org.kohsuke.bali.automaton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.msv.grammar.Expression;
import com.sun.msv.util.StringPair;



/**
 * Binary tree automaton.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class TreeAutomaton {
    public TreeAutomaton( Map _nameCodes ) {
        this.nameCodes = _nameCodes;
    }
    
    /** All states of this binary automaton. */
    private List states = new ArrayList();
    /** Obtains all the states in the order of their id. */
    public State[] getStates() { return (State[])states.toArray(new State[states.size()]); }
    
    private State initialState;
    /** Gets the initial state of this automaton. */
    public State getInitialState() { return initialState; }
    
    /** Gets the number of states. */
    public int countStates() { return states.size(); }
    
    /** Gets the total number of transitions. */
    public int countTransitions() {
        int cnt=0;
        for (Iterator itr = states.iterator(); itr.hasNext();) {
            State s = (State) itr.next();
            cnt += s.countTransitions();
        }
        return cnt;
    }

    /** used to assign numbers to states. */
    private int iota = 0;
    
    /**
     * A map from StringPair to Integer that represents 
     * name code.
     */
    private final Map nameCodes;

    /** Obtains the name code from the given name. */
    public int getNameCode( String nsUri, String localName ) {
        return getNameCode( new StringPair(nsUri,localName) );
    }

    /** Obtains the name code from the given name. */
    public int getNameCode( StringPair name ) {
        Integer r;
        r = (Integer)nameCodes.get(name);
        if(r!=null) return r.intValue();
        
        r = (Integer)nameCodes.get(new StringPair(name.namespaceURI,IMPOSSIBLE));
        if(r!=null) return r.intValue();
        
        r = (Integer)nameCodes.get(WILDCARD);
        if(r!=null) return r.intValue();
        
        return -1;  // this represents NaN.
    }
    
    /**
     * Obtains all the (uri,local) pairs with distinct name codes.
     * uri and local may be IMPOSSIBLE to represent a wildcard.
     */
    public StringPair[] listNameCodes() {
        return (StringPair[]) nameCodes.keySet().toArray(new StringPair[nameCodes.size()]);
    }


    /** Creates a new state in this tree automaton and returns it. */
    public State createState( Expression exp, boolean isFinal, State nextState ) {
        State s = new State(exp,isFinal,iota,nextState);
        states.add(s);

        if( iota == 0 )
            initialState = s;

        iota++;
        
        return s;
    }
    
    

    /** Invalid name token value. */
    public static final String IMPOSSIBLE = "\u0000";
    
    /** Constant value that represents "*:*". */
    public static final StringPair WILDCARD = new StringPair( IMPOSSIBLE, IMPOSSIBLE);
}
