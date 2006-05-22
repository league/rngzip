package org.kohsuke.bali.automaton.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import org.kohsuke.bali.automaton.*;
import org.kohsuke.bali.datatype.DatatypeImpl;
import org.relaxng.datatype.Datatype;

import com.sun.msv.datatype.xsd.StringType;
import com.sun.msv.datatype.xsd.TokenType;
import com.sun.msv.grammar.*;
import com.sun.msv.grammar.util.NameClassCollisionChecker;
import com.sun.msv.grammar.util.NameClassSimplifier;

/**
 * Builds a BinaryTreeAutomaton object from a Grammar object.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class TreeAutomatonBuilder {
    /** Builds a binary tree automaton from a grammar. */
    public static TreeAutomaton build( Grammar grammar,
        boolean optimizeIgnorableAttribute,
        boolean useEpsilonTransition,
        boolean optimizeMixed ) throws TooComplicatedException {
        
        return new TreeAutomatonBuilder( grammar,
            optimizeIgnorableAttribute,
            useEpsilonTransition,
            optimizeMixed ).doBuild();
    }
    private TreeAutomaton doBuild() throws TooComplicatedException {
        
        // set the first job in the queue
        getState( grammar.getTopLevel().getExpandedExp(pool) );
        
        while(!queue.isEmpty()) {
            Expression e = (Expression)queue.pop();
            State st = (State)states.get(e);
            
            // Finds all transition from the given state.
//            if(st.nextState!=null)
//                ((SequenceExp)e).exp1.visit(new TransitionBuilder(st));
//            else
                e.visit(new TransitionBuilder(st));
        }
        
        return result;
    }
    
    
    
    private TreeAutomatonBuilder( Grammar grammar,
        boolean optimizeIgnorableAttribute,
        boolean useEpsilonTransition,
        boolean optimizeMixed ) throws TooComplicatedException {
        
        this.grammar = grammar;
        this.optimizeIgnorableAttribute = optimizeIgnorableAttribute;
        this.useEpsilonTransition = useEpsilonTransition;
        this.optimizeMixed = optimizeMixed;
        pool = grammar.getPool();
        nameClassEncoder = NameClassEncoder.build(grammar);
        result  = new TreeAutomaton(nameClassEncoder.literals);
        nullChecker = new NullabilityChecker(optimizeIgnorableAttribute);
        allNegative = new NameSignature(AnyNameClass.theInstance, 0,0, this.nameClassEncoder );
     }
    
    /** The grammar object from which we are building a binary tree automaton. */
    private final Grammar grammar;
    private final ExpressionPool pool;
    
    /** Optimizes ignorable attribute if true. */
    private final boolean optimizeIgnorableAttribute;
    
    /** Reduce the number of transitions by sharing transitions across states. */
    private final boolean useEpsilonTransition;
    
    /** Optimize &lt;mixed> */
    private final boolean optimizeMixed;
    
    /** The result automaton. */
    private final TreeAutomaton result;
    
    /** Map from Expression to State. */
    private final HashMap states = new HashMap();
    
    /** NameClass Encoder */
    private final NameClassEncoder nameClassEncoder;

    /**
     * Map from a State to a State.
     * Used for non-existent attribute transition from the head of a content model.
     *  TODO: better documentation
     */
    private final HashMap headStates = new HashMap();
    
    private final NullabilityChecker nullChecker;
    
    /**
     * Returns true if the state that corresponds to the given expression
     * can make use of an epsilon transition.
     */
    private boolean isSplittable( Expression e ) {
        // don't perform this optimization unless it's turned on.
        if( useEpsilonTransition && e instanceof SequenceExp ) {
            SequenceExp se = (SequenceExp)e;
            if( se.getChildren()[0].visit(nullChecker) )
                return true;
        }
        return false;
    }
    
    private Expression getSequenceTail( SequenceExp e ) {
        Expression[] exps = e.getChildren();
        Expression r = Expression.epsilon;
        for( int i=1; i<exps.length; i++ )
            r = pool.createSequence(r,exps[i]);
        return r;
    }
    
    /** Gets or creates the State object corresponding to the given expression. */
    private State getState( Expression e ) {
        if(e==null)     throw new NullPointerException();
        
        State st = (State)states.get(e);
        if(st==null) {
            if( isSplittable(e) ) {
                SequenceExp se = (SequenceExp)e;
                states.put( se, st=result.createState(
                    e,
                    e.visit(nullChecker),
                    getState(getSequenceTail(se)) ));
                queue.push(e);
            } else {
                states.put( e, st=result.createState( e, e.visit(nullChecker), null) );
                queue.push(e);
            }
        }
        return st;
    }
    
    /**
     * Returns true if the given state represents the given expression.
     */
    private boolean isEqual( State s, Expression e ) {
        return states.get(e)==s;
    }

    /** Special NameSignature array that has "*:*" */
    private final NameSignature allNegative;
    
    private final NameClassCollisionChecker collisionChecker = new NameClassCollisionChecker();
    
    /** Combines all the name classes in the given collection. */
    private NameClass combine( Collection col ) {
        NameClass nc = new NotNameClass(AnyNameClass.theInstance);
        for( Iterator itr =  col.iterator(); itr.hasNext(); ) {
            NameSignature ns = (NameSignature)itr.next();
            
            nc = new ChoiceNameClass( nc, ns.nameClass );
        }
        return NameClassSimplifier.simplify(nc);
    }
    
    private void refine( Set negatives, Set positives ) {
        // computes the sum of all positive names.
        NameClass pos = combine(positives);
        
        // refine negatives
        ArrayList unusedNegatives = new ArrayList();
        for( Iterator itr = negatives.iterator(); itr.hasNext(); ) {
            NameSignature ns = (NameSignature)itr.next();
            if( new DifferenceNameClass( ns.nameClass, pos ).isNull() ) 
                unusedNegatives.add(ns);
            // if ns is included in the positives, ns is not contributing
            // to the outcome. thus remove it.
        }
        negatives.removeAll(unusedNegatives);

        // computes the sum of all positive names.
        NameClass neg = combine(negatives);
        
        // refine positives
        ArrayList unusedPositives = new ArrayList();
        for( Iterator itr = positives.iterator(); itr.hasNext(); ) {
            NameSignature ns = (NameSignature)itr.next();
            if( !collisionChecker.check( ns.nameClass, neg ) )
                unusedPositives.add(ns);
            // if ns doesn't have any intersection with negatives,
            // it's not contributing.
        }
        positives.removeAll(unusedPositives);
    }
    
    /**
     * Computes the initial state for the content model of a given ElementExp.
     * 
     * This method adds non-existent attribute transition at the top of
     * the content model if necessary.
     */
    private State getContentModelHeadState( ElementExp exp ) {
        State contentModelTop = getState(exp.contentModel.getExpandedExp(pool));
        
        Set neg = new HashSet();
        neg.add(allNegative);
        Set pos = AttNameSigCollector.collect(exp.contentModel,nameClassEncoder);
        
        refine( neg, pos );
        
        if(neg.isEmpty()) {
            return contentModelTop;
        } else {
            State st = (State)headStates.get(contentModelTop);
            if(st==null) {
                headStates.put( contentModelTop,
                    st=result.createState(exp.contentModel.getExpandedExp(pool),false,null) );
                st.addTransition( new NonExistentAttributeAlphabet(neg,pos),
                    null, contentModelTop);
            }
            
            return st;
        }
    }

    /** List of unprocessed expressions. */
    private final Stack queue = new Stack();
    
    private static final DataAlphabet anyStringAlphabet =
        new DataAlphabet(new DatatypeImpl("","string",StringType.theInstance));
    
    
    /**
     * Visits an expression (a state) and builds all transitions from it
     * by computing derivatives.
     */
    private final class TransitionBuilder implements ExpressionVisitorVoid {
        TransitionBuilder( State st ) {
            this.state = st;
            this.tail = Expression.epsilon;
        }
        
        /** We are adding transitions that leaves this state. */
        private final State state;
        
        private Expression tail;
        
        /** This method is true if we are inside &lt;mixed>. */
        private boolean inMixed;
        
        private State getState( Expression e ) {
            if( inMixed )
                e = pool.createMixed(e);
            return TreeAutomatonBuilder.this.getState(e);
        }
        
        public void onAttribute(AttributeExp exp) {
            state.addTransition( new AttributeAlphabet(
                nameClassEncoder.getSignature(exp.nameClass),false),
                getState(exp.exp.getExpandedExp(pool)), getState(tail) );
        }
        

        public void onChoice(ChoiceExp exp) {
            Expression t = tail;
            
            if( optimizeIgnorableAttribute ) {
                /* look for
                    <choice>
                      <empty/>
                      <attribute>
                        <text/>
                      </attribute>
                    </choice>
                   
                   and avoid generating transitions for them.
                */
                if( Util.isIgnorableOptionalAttribute(exp) )
                    return;
            }
            
            
            
            // check attributes in the branch
            Set nc1a = AttNameSigCollector.collect( exp.exp1, nameClassEncoder );
            Set nc2a = AttNameSigCollector.collect( exp.exp2, nameClassEncoder );
            
            Set nc1b = new HashSet(nc1a);
            Set nc2b = new HashSet(nc2a);
            
            refine( nc1a, nc2a );
            if( nc1a.isEmpty() ) {
                tail = t;
                exp.exp2.visit(this);
            } else {
                state.addTransition( new NonExistentAttributeAlphabet(nc1a,nc2a),
                    null, getState(pool.createSequence(exp.exp2,t)) );
            }
            
            refine( nc2b, nc1b );
            if( nc2b.isEmpty() ) {
                tail = t;
                exp.exp1.visit(this);
            } else {
                state.addTransition( new NonExistentAttributeAlphabet(nc2b,nc1b),
                    null, getState(pool.createSequence(exp.exp1,t)) );
            }
        }
        
        public void onElement(ElementExp exp) {
            state.addTransition( new ElementAlphabet(
                nameClassEncoder.getSignature(exp.getNameClass())),
                getContentModelHeadState(exp), getState(tail) );
        }

        public void onOneOrMore(OneOrMoreExp exp) {
            // find an attribute declaration
            AttributeExp attDecl = findAttDecl(exp.exp);
            
            if(attDecl==null) {
                tail = pool.createSequence( pool.createOptional(exp), tail );
                exp.exp.visit(this);
            } else {
                // this is equivalent to deriv(exp,@attDecl);
                Expression deriv = pool.createOptional(exp);
                
                NameSignature sig = (NameSignature)nameClassEncoder.getSignature(attDecl.nameClass);
                
                Expression derivEndAtt = pool.createSequence(
                    replace(deriv,attDecl,Expression.nullSet), tail);
                Expression expEndAtt   = pool.createSequence(
                    replace(exp,  attDecl,Expression.nullSet), tail);
                
                if(derivEndAtt!=Expression.nullSet)
                    state.addTransition(new AttributeAlphabet(sig,true),
                        getState(attDecl.exp), getState(derivEndAtt));
                
                if(expEndAtt!=Expression.nullSet)
                    state.addTransition(new NonExistentAttributeAlphabet(
                        new NameSignature[]{sig},new NameSignature[0]),
                        null, getState(expEndAtt));
            }
        }

        public void onMixed(MixedExp exp) {
            if( optimizeMixed ) {
                // optimize <mixed> by eagerly extend <interleave>
                final boolean oldInMixed = inMixed;
                inMixed = true;
                
                // add a loop-back transition.
                state.addTransition( anyStringAlphabet,
                    getState(Expression.nullSet), state );
                
                exp.exp.visit(this);
                
                inMixed = oldInMixed;
            } else {
                // this is the normal mode
                pool.createInterleave( exp.exp, Expression.anyString ).visit(this);
            }
        }

        public void onList(ListExp exp) {
            state.addTransition( ListAlphabet.theInstance,
                getState(exp.exp), getState(tail) );
        }

        public void onRef(ReferenceExp exp) {
            exp.exp.visit(this);
        }

        public void onOther(OtherExp exp) {
            exp.exp.visit(this);
        }

        public void onEpsilon() {
            ;
        }

        public void onNullSet() {
            ; // no transition from nullSet
        }

        public void onAnyString() {
            State target;
            
            if( isEqual(state,tail) )
                // optimization.
                // if d(P) = <text/>,P then
                //   d(<text/>,P) \supseteq d(P)
                target = getState(tail);
            else
                // normal case -- derivative of <text/> is still <text/>
                target = getState(pool.createSequence(Expression.anyString,tail));
            
            state.addTransition( anyStringAlphabet,
                getState(Expression.nullSet), target );
        }

        public void onSequence(SequenceExp exp) {
            Expression t = tail;
            Expression stail = getSequenceTail(exp);
            if( useEpsilonTransition && state.nextState!=null
            &&  states.get(stail)==state.nextState ) {
                //don't visit the tail of the sequence.
                tail = pool.createSequence(stail,t);
                exp.getChildren()[0].visit(this);
            } else {
                if( exp.exp1.visit(nullChecker) )
                    exp.exp2.visit(this);
                
                tail = pool.createSequence( exp.exp2, t );
                exp.exp1.visit(this);
            }
        }

        public void onData(DataExp exp) {
            Expression except = exp.except;
            if(except==null)    except = Expression.nullSet;
            
            state.addTransition( new DataAlphabet(exp.dt), getState(except), getState(tail) );
        }

        public void onValue(ValueExp exp) {
            // MSV uses TokenType if <value> doesn't have @type.
            // replace it with our DatatypeImpl.
            Datatype dt = exp.dt;
            if( dt instanceof TokenType )   dt = new DatatypeImpl("","token",TokenType.theInstance);
            
            state.addTransition( new ValueAlphabet(dt,exp.value), null, getState(tail) );
        }

        public void onConcur(ConcurExp exp) {
            throw new UnsupportedOperationException("<concur> is not supported");
        }

        public void onInterleave(InterleaveExp exp) {
            state.addTransition(
                new InterleaveAlphabet(
                    getState(tail),
                    AttNameCombiner.collect(exp.exp1),
                    AttNameCombiner.collect(exp.exp2),
                    TextFinder.find(exp.exp1)
                ),
                getState(exp.exp1), getState(exp.exp2) );
        }
    }
    
    
    
    /**
     * Finds one attribute declaration in the given expression, or null.
     */
    private AttributeExp findAttDecl( Expression exp ) {
        return (AttributeExp)exp.visit(new ExpressionVisitorExpression() {
            public Expression onAttribute(AttributeExp exp) { return exp; }
            public Expression onChoice(ChoiceExp exp) {
                Expression e = exp.exp1.visit(this);
                if(e!=null)     return e;
                return exp.exp2.visit(this);
            }
            public Expression onElement(ElementExp exp) { return null; }
            public Expression onOneOrMore(OneOrMoreExp exp) { return exp.exp.visit(this); }
            public Expression onMixed(MixedExp exp) { return exp.exp.visit(this); }
            public Expression onList(ListExp exp) { return null; }
            public Expression onRef(ReferenceExp exp) { return exp.exp.visit(this); }
            public Expression onOther(OtherExp exp) { return exp.exp.visit(this); }
            public Expression onEpsilon() { return null; }
            public Expression onNullSet() { return null; }
            public Expression onAnyString() { return null; }
            public Expression onSequence(SequenceExp exp) {
                // since this method can be only used inside <oneOrMore>, if we see
                // <group> then there's no chance that we'll see <attribute>.
                return null;
            }
            public Expression onData(DataExp exp) { return null; }
            public Expression onValue(ValueExp exp) { return null; }
            public Expression onConcur(ConcurExp exp) { throw new InternalError(); }
            public Expression onInterleave(InterleaveExp exp) {
                return null;    // the same reason as the onSequence method.
            }
        });
    }
    
    /**
     * Replaces the occurence of "from" inside "exp" to "to".
     */
    private Expression replace( Expression exp, final AttributeExp from, final Expression to ) {
        return exp.visit(new ExpressionCloner(pool){
            public Expression onRef( ReferenceExp exp ) { return exp.exp.visit(this); }
            public Expression onOther( OtherExp exp ) { return exp.exp.visit(this); }
            public Expression onAttribute( AttributeExp exp ) {
                if(exp==from)   return to;
                else            return exp;
            }
            public Expression onElement( ElementExp exp ) { return exp; }
        });
    }
}
