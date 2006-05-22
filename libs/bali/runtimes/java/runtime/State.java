package runtime;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.relaxng.datatype.ValidationContext;

/**
 * A state in the compiled automaton.
 * 
 * <p>
 * Since states are so frequently created and discarded,
 * we use reference count to reuse State objects.
 * <p>
 * Reference Counting rule of thumb:
 * <ol>
 *  <li>If a method wants to keep a refnerece to one of its parameter,
 *       it needs to call addRef explicitly.
 *  <li>If a method needs to return a State, it must first addRef the returned State.
 *  <li>If a method returns a State, it's a caller's responsibility to release it.
 * 
 *  <li>partialResult is the only exception.
 * </ol>
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class State {
    
    /**
     * Indicates how the state behaves to text tokens.
     * 
     * <p>
     * This is an optimization hint to the validatelet.
     * 
     * @return
     *      0   whitespace alone is allowed
     *      1   text is ignorable. calling the text method
     *          is guaranteed to return the same state.
     *      2   state is sensitive to the contents of text token.
     */
    public final int textSensitivity;
    
    /**
     * The cached return value from the endElementFast method.
     */
    private State cachedEndElementFast = null;
    
    
    protected State( int _textSensitivity ) {
        this.textSensitivity = _textSensitivity;
    }
    
    /**
     * Performs a transition by a start element event.
     */
    public abstract State startElement( int nameCode, AttributesSet attributes, State partialResult, StateFactory factory );
    
    /**
     * Performs a transition by an end element event.
     * 
     * @param attributes
     *      attributes of the parent element of the element being closed.
     */
    public abstract State endElement( AttributesSet attributes, State partialResult, StateFactory factory );
    
    /**
     * Optimized endElement method, which is equivalent to
     * <code>endElement( AttributesSet.empty, State.emptySet, factory )</code>.
     */
    public final State endElementFast( StateFactory factory ) {
        if( cachedEndElementFast==null )
            cachedEndElementFast = endElement( AttributesSet.empty, State.emptySet, factory );
        return cachedEndElementFast;
    }
    
    /**
     * Expands StateSet by taking applicable attribute transitions.
     */
    public abstract State expand( AttributesSet attributes, State partialResult, StateFactory factory );
    
    /**
     * Equivalent to
     * <code>expand(attributes,emptySet.addRef(),factory);</code>
     */
    public final State expandFast( AttributesSet attributes, StateFactory factory ) {
        if( isExpandable() )
            return expand( attributes, emptySet, factory );
        else
            return this;
    }
    
    /**
     * Equivalent to the expand method but faster.
     */
    public final State expandFast( AttributesSet attributes, State partialResult, StateFactory factory ) {
        if( isExpandable() )
            return expand( attributes, partialResult, factory );
        else
            return factory.makeChoice( partialResult, this );
    }
    
    /**
     * Performs a transition by a text chunk.
     */
    public abstract State text( String value, boolean ignorable, ValidationContext context,
        AttributesSet attributes, State partialResult, StateFactory factory );
    
    public State wrapAfterByAfter( State newThen, State partialResult, StateFactory factory ) {
        throw new InternalError(this.getClass().toString()); // this can't happen for Interleave/SingleState.
    }
    public State wrapAfterByInterleaveLeft ( State lhs, Transition.Interleave alphabet, State partialResult, StateFactory factory ) {
        throw new InternalError(this.getClass().toString()); // this can't happen for Interleave/SingleState.
    }
    public State wrapAfterByInterleaveRight( State rhs, Transition.Interleave alphabet, State partialResult, StateFactory factory ) {
        throw new InternalError(this.getClass().toString()); // this can't happen for Interleave/SingleState.
    }
    
    /**
     * Returns true if the current state is a final state.
     */
    public abstract boolean isFinal();
    
    
    
    
    public static final int TEXT_WHITESPACE_ONLY = 0;
    public static final int TEXT_IGNORABLE = 1;
    public static final int TEXT_SENSITIVE = 2;
    
    /**
     * Returns true if the expand method can return some
     * other states. If this method returns false, the expand
     * method is guaranteed to be equivalent to
     * makeChoice( partialResult, this );
     * 
     * <p>
     * This is an optimization hint.
     */
    public abstract boolean isExpandable();
    
    /**
     * Returns true if the choice tree contains the given state.
     */
    public boolean contains( State s ) {
        return this==s;
    }

    public final String toString() {
        return toString(0);
    }
    /**
     * @param parentPrecedence
     *      Operator precedence of the parent state.
     *      Bigger number means stronger precedence.
     *      1:AfterState, 2:ChoiceState, 3:InterleaveState
     */
    public abstract String toString( int parentPrecedence );
    
    protected static String parenthesis( String s ) {
        return '('+s+')';
    }
    
    
    
    
    
    /** Singletone emptySet instance. */
    protected static State emptySet = new Empty();
    
    private static final class Empty extends State {
        protected Empty() {
            // derivative of <notAllowed/> is always <notAllowed/>
            super(TEXT_IGNORABLE);
        }
        
        public State endElement( AttributesSet attributes, State partialResult, StateFactory factory ) {
            return partialResult;
        }
        public State expand(AttributesSet attributes, State partialResult, StateFactory factory) {
            return partialResult;
        }
        public boolean isExpandable() {
            return true;
        }
        public boolean isFinal() {
            return false;
        }
        public State startElement( int nameCode, AttributesSet attributes, State partialResult, StateFactory factory ) {
            return partialResult;
        }
        public State text( String value, boolean ignorable, ValidationContext context, AttributesSet attributes, State partialResult, StateFactory factory) {
            return partialResult;
        }
        public State wrapAfterByAfter( State newThen, State partialResult, StateFactory factory ) {
            return partialResult;
        }
        public State wrapAfterByInterleaveLeft ( State lhs, Transition.Interleave alphabet, State partialResult, StateFactory factory ) {
            return partialResult;
        }
        public State wrapAfterByInterleaveRight( State rhs, Transition.Interleave alphabet, State partialResult, StateFactory factory ) {
            return partialResult;
        }
        public String toString( int p ) { return "#err"; }
    }
    
    
    
    
    
    
    
    
    

    
    
    
    
    
    
    
    
    public final static class After extends State {
        public final State child;
        public final State then;
        
        public After( State c, State t ) {
            super(c.textSensitivity);

            this.child=c;
            this.then=t;
        }
    
        public State startElement(
            int nameCode, AttributesSet attributes, State partialResult, StateFactory factory ) {
            
            State s = child.startElement( nameCode, attributes, emptySet, factory );
            State r = s.wrapAfterByAfter( then, partialResult, factory );
            
            return r;
        }
    
        public State endElement( AttributesSet attributes, State partialResult, StateFactory factory ) {
            if( child.isFinal() )   return then.expandFast( attributes, partialResult, factory );
            else                    return partialResult;
        }
    
        public State expand( AttributesSet attributes, State partialResult, StateFactory factory ) {
            State s1 = child.expand( attributes, emptySet, factory );
//            // optimization.
//            if(s1==child) {
//                s1;   // we won't use s1.
//                return factory.makeChoice( partialResult, this );
//            }
//            
            State s2 = factory.makeAfter( s1, then );
            State r  = factory.makeChoice( partialResult, s2 );
            
            return r;
            // don't expand the "then" states because it needs different attributes.
            // we'll expand them at the endElement method.
        }

        public boolean isExpandable() {
            return child.isExpandable();
        }
        
        public State text( String value, boolean ignorable, ValidationContext context,
            AttributesSet attributes, State partialResult, StateFactory factory ) {
            
            State s1 = child.text( value, ignorable, context, attributes, emptySet, factory );
            State s2 = factory.makeAfter( s1, then );
            State r = factory.makeChoice( partialResult, s2 );
            
            return r;
        }
        
        public State wrapAfterByAfter( State newThen, State partialResult, StateFactory factory ) {
            State s1 = factory.makeAfter( then, newThen );
            State s2 = factory.makeAfter( child, s1 );
            State r  = factory.makeChoice( partialResult, s2 );
            
            return r;
        }
        public State wrapAfterByInterleaveLeft( State lhs, Transition.Interleave alphabet, State partialResult, StateFactory factory ) {
            State s1 = factory.makeInterleave( lhs, then, alphabet );
            State s2 = factory.makeAfter( child, s1 );
            State r  = factory.makeChoice( partialResult, s2 );
            
            return r;
        }
        public State wrapAfterByInterleaveRight( State rhs, Transition.Interleave alphabet, State partialResult, StateFactory factory ) {
            State s1 = factory.makeInterleave( then, rhs, alphabet );
            State s2 = factory.makeAfter( child, s1 );
            State r  = factory.makeChoice( partialResult, s2 );
            
            return r;
        }
        
        
    
        public boolean contains( State s ) {
            if(!(s instanceof After)) return false;
            
            After rhs = (After)s;
            
            // TODO needs more generalization
            return this.child.contains(rhs.child)
                && rhs.then.contains(this.then)
                && this.then.contains(rhs.then);
        }
    
        public boolean isFinal() {
            return child.isFinal();
        }

        
        
        
        public String toString( int p ) {
            String s = child.toString(1)+" then "+then.toString(1);
            if( p>1 )   s = parenthesis(s);
            return s;
        }
    }
    
    
    
    
    public static final class Choice extends State {
        public final State lhs,rhs;
        
        
        private static int getTextSensitivity( State lhs, State rhs ) {
            int l = lhs.textSensitivity;
            int r = rhs.textSensitivity;
            if( l==TEXT_SENSITIVE || r==TEXT_SENSITIVE || l!=r )
                return TEXT_SENSITIVE;
            return l;
        }
        
        public Choice(  State l, State r ) {
            super(getTextSensitivity(l,r));
            this.lhs=l;
            this.rhs=r;
        }
        
        public State startElement(
            int nameCode, AttributesSet attributes, State partialResult, StateFactory factory ) {
            
            State s1 = lhs.startElement(nameCode,attributes,partialResult,factory);
            State r  = rhs.startElement(nameCode,attributes,s1,factory);
            
            return r;
        }
        
        public State endElement( AttributesSet attributes, State partialResult, StateFactory factory ) {
            State s1 = lhs.endElement( attributes, partialResult, factory );
            State r  = rhs.endElement( attributes, s1, factory );
            
            return r;
        }
        
        public State expand( AttributesSet attributes, State partialResult, StateFactory factory ) {
            State s1 = lhs.expand( attributes, partialResult, factory );
            State r  = rhs.expand( attributes, s1, factory );
            
            return r;
        }

        public boolean isExpandable() {
            return lhs.isExpandable() || rhs.isExpandable();
        }
        
        public State text( String value, boolean ignorable, ValidationContext context,
            AttributesSet attributes, State partialResult, StateFactory factory ) {
            
            State s1 = lhs.text(value,ignorable,context,attributes,partialResult,factory);
            State r  = rhs.text(value,ignorable,context,attributes,s1,factory);
            
            return r;
        }
    
        public State wrapAfterByAfter( State newThen, State partialResult, StateFactory factory ) {
            
            State s1 = lhs.wrapAfterByAfter( newThen, partialResult, factory );
            State r  = rhs.wrapAfterByAfter( newThen, s1, factory );
            
            return r;
        }
        
        public State wrapAfterByInterleaveLeft ( State newLhs, Transition.Interleave alphabet, State partialResult, StateFactory factory ) {
            State s1 = lhs.wrapAfterByInterleaveLeft( newLhs, alphabet, partialResult, factory );
            State r  = rhs.wrapAfterByInterleaveLeft( newLhs, alphabet, s1, factory );
            
            return r;
        }
        public State wrapAfterByInterleaveRight( State newRhs, Transition.Interleave alphabet, State partialResult, StateFactory factory ) {
            State s1 = lhs.wrapAfterByInterleaveRight( newRhs, alphabet, partialResult, factory );
            State r  = rhs.wrapAfterByInterleaveRight( newRhs, alphabet, s1, factory );
            
            return r;
        }
        
        public boolean contains( State s ) {
            return lhs.contains(s) || rhs.contains(s);
        }
        
        public boolean isFinal() {
            return lhs.isFinal() || rhs.isFinal();
        }
    
        
    
        public String toString( int p ) {
            String s = lhs.toString(2)+"|"+rhs.toString(2);
            if( p>2 )   s = parenthesis(s);
            return s;
        }
    }





    public static final class Interleave extends State {
        public final State lhs,rhs;
        public final Transition.Interleave alphabet;    // TODO: it might be better to extend it.


        public Interleave( State l, State r, Transition.Interleave a ) {
            super( a.textToLeft ? l.textSensitivity : r.textSensitivity );
            
            // caller needs to addRef
            this.lhs=l;
            this.rhs=r;
            this.alphabet=a;
        }
    
        public State startElement(
            int nameCode, AttributesSet attributes, State result, StateFactory factory ) {
            
            State l = lhs.startElement( nameCode, attributes, emptySet, factory );
            State r = rhs.startElement( nameCode, attributes, emptySet, factory );

            State x1 = r.wrapAfterByInterleaveLeft (lhs,alphabet, result, factory );
            State x2 = l.wrapAfterByInterleaveRight(rhs,alphabet, x1, factory );
            
            return x2;
        }
    
        public State endElement( AttributesSet attributes, State partialResult, StateFactory factory ) {
            // this method can never be called because
            // InterleaveState will not appear above AfterState.
            throw new InternalError();
        }
    
        public State expand( AttributesSet attributes, State partialResult, StateFactory factory ) {
            State l = lhs.expand( attributes, emptySet, factory );
            State r = rhs.expand( attributes, emptySet, factory );
            
            State x1 = factory.makeInterleave( l, r, alphabet );
            State x2 = factory.makeChoice(  partialResult, x1 );
            
            return x2;
        }

        public boolean isExpandable() {
            return lhs.isExpandable() || rhs.isExpandable();
        }
        
        public State text( String value, boolean ignorable, ValidationContext context,
            AttributesSet attributes, State result, StateFactory factory ) {
            
            State i;
            
            if( alphabet.textToLeft ) {
                State t = lhs.text( value, ignorable, context, attributes, emptySet, factory );
                i = factory.makeInterleave( t, rhs, alphabet );
            } else {
                State t = rhs.text( value, ignorable, context, attributes, emptySet, factory );
                i = factory.makeInterleave( lhs, t, alphabet );
            }
                        
            result = factory.makeChoice( result, i );
                    
            if( i instanceof Interleave && ((Interleave)i).canJoin() )
                result = alphabet.join.expandFast( attributes, result, factory );
            
            return result;
        }
        
        public boolean isFinal() {
            // two children must be joinable and the target state must be final
            return canJoin() && alphabet.join.isFinal;
        }
        
        private boolean canJoin() {
            return lhs.isFinal() && rhs.isFinal();
        }
    
    
        public String toString( int p ) {
            String s = lhs.toString(3)+"&"+rhs.toString(3)+"->#"+alphabet.join.id;
            if( p>=2 )  s = parenthesis(s);
            return s;
        }
    }
    
    
    
    
    
    public static final class Single extends State {
        public Single( int _textSensitivity, boolean _isFinal, boolean _isPersistent, int _id ) {
            super( _textSensitivity );
            this.isFinal = _isFinal;
            this.isPersistent = _isPersistent;
            this.id = _id;
        }
        
        public void dispose() { throw new InternalError(); }
        
        
        public final int id;    // id of this state. used for debugging only.
        
        public final boolean isFinal, isPersistent;
        
        // considered to be final.
        public boolean isExpandable;
        
        // reference to the first transition. considered as immutable
        public Transition.Att aTr;
        public Transition.Data dTr;
        public Transition.Element eTr;
        public Transition.Interleave iTr;
        public Transition.List lTr;
        public Transition.NoAtt nTr;
        public Transition.Value vTr;
        
        // element transition with mask==-1 sorted by the order of test.
        public Transition.Element[] quickETr;
        

        public State startElement(
            int nameCode, AttributesSet attributes, State result, StateFactory factory ) {
            
            {// check quick element transition by a binary search
                int base = 0;
                int size = quickETr.length;
                
                Transition.Element e=null;
                
                outerWhile:
                while(size!=0) {
                    int mid = size/2;
                    
                    e = quickETr[mid+base];
                    int test = e.test;
                    if(test==nameCode)
                        break;  // found
                
                    if(test<nameCode) {
                        // go to right
                        base += mid+1;
                        size -= mid+1;
                    } else {
                        // go to left
                        size = mid;
                    }

                    if( size <= 4 ) {
                        // switch to linear search
                        for( int i=0; i<size; i++ ) {
                            e = quickETr[base+i];
                            if(e.test==nameCode)
                                break outerWhile;  // found
                        }
                        e = null;   // not found
                        break;
                    }
                }
                
                // process neighbors as well
                for( ; e!=null; e=e.next )
                    result = e.startElement( result, attributes, factory );
            }
            
            // then check the rest
            for( Transition.Element e=eTr; e!=null; e=e.next )
                if( e.accepts(nameCode) )
                    result = e.startElement( result, attributes, factory );
            
            return result;
        }
        
        public State endElement( AttributesSet attributes, State partialResult, StateFactory factory ) {
            // this method can never be called because
            // SingleState will not appear above AfterState.
            throw new InternalError();
        }
        
        
        public State text( String value, boolean ignorable, ValidationContext context,
            AttributesSet attributes, State result, StateFactory factory ) {
            
            if(ignorable) {   // this text is ignorable
                result = factory.makeChoice( result, this );
            }
            
            for( Transition.Data da=dTr; da!=null; da=da.next ) {
                
                // data transition can be taken when it's accepted by the datatype AND
                // the except expression fails.
                if( da.datatype.isValid(value,context) ) {
                    State s = da.left.text( value, ignorable, context, AttributesSet.empty, emptySet, factory );
                    boolean fin = s.isFinal();
                    
                    if( !fin )
                        result = da.right.expandFast( attributes, result, factory );
                }
            }
            
            for( Transition.Value va=vTr; va!=null; va=va.next ) {
                
                if( va.accepts(value,context) )
                    result = va.right.expand(attributes,result,factory);
            }
            
            for( Transition.List la=lTr; la!=null; la=la.next ) {
                
                StringTokenizer tokens = new StringTokenizer(value);
                // a list can't contain interleave, so no need to expand
                State child = la.left;
                
                while(tokens.hasMoreTokens() && child!=null) {
                    String token = tokens.nextToken();
                    child = child.text( token, token.trim().length()==0, context,
                        AttributesSet.empty, emptySet, factory );
                }
                
                if( child.isFinal())
                    result = la.right.expand( attributes, result, factory );
            }
            
            return result;
        }
        
    
    
        public State expand( AttributesSet attributes, State result, StateFactory factory ) {

            if( result.contains(this) )   return result;  // no need to expand more
            
            if( isPersistent )
                result = factory.makeChoice( result, this );
            
            int attSize = attributes.size();
            
            if( attSize!=0 ) {
                for( Transition.Att aa = aTr; aa!=null; aa=aa.next ) {
                    if( attributes.matchs(aa,factory) )
                        result = aa.right.expandFast( attributes, result, factory );
                }
            }
            
            for( Transition.NoAtt nea = nTr; nea!=null; nea=nea.next ) {
                
                int j;
                for( j=attSize-1; j>=0; j-- ) {
                    int nc = attributes.getName(j);
                    if( nea.accepts(nc) )
                        // this attribute is prohibited.
                        break;
                }
                if(j==-1)
                    result = nea.right.expandFast( attributes, result, factory );
            }
            
            for( Transition.Interleave ia = iTr; ia!=null; ia=ia.next ) {
                result = factory.makeChoice( result, factory.makeInterleave(
                    ia.left. expandFast( attributes, factory ),
                    ia.right.expandFast( attributes, factory ),
                    ia ));
            }
            
            return result;
        }
    
        
        
        public boolean contains( State s ) {
            return this==s;
        }
    
        public boolean isFinal() {
            return isFinal;
        }

        public boolean isExpandable() {
            return isExpandable;
        }
        
        public String toString( int p ) {
            if( aTr==null && dTr==null && eTr==null && iTr==null && lTr==null && nTr==null && vTr==null && quickETr==null && isFinal )
                return "#eps";
            return "#"+id;
        }
    }
    
    
    
    
}
