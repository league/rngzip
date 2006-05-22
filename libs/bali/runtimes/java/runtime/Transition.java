package runtime;
import org.relaxng.datatype.Datatype;
import org.relaxng.datatype.ValidationContext;

/**
 * 
 * Just a wrapper to enclose different types of transitions.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class Transition {
    private Transition() {}
    
    /** Transition by attribute. */
    public final static class Att {
        public Att( int mask, int test, boolean repeated, State.Single left, State.Single right, Att next ) {
            this.mask = mask;
            this.test = test;
            this.repeated = repeated;
            this.left = left;
            this.right = right;
            this.next = next;
        }
        /** Name signature. */
        private final int mask,test;
        /** True if this alphabet is of the form @X+. */
        public final boolean repeated;
        /** Left and right states. */
        public final State.Single left,right;
        
        /** Next transition of this kind, or null. */
        public final Att next;
        
        /** Returns true if this alphabet accets the given name code. */
        public boolean accepts( int nameCode ) {
            return (nameCode&mask)==test;
        }
    }
    
    /** Transition by data. */
    public final static class Data {
        public Data( Datatype dt, State.Single left, State.Single right, Data next ) {
            this.datatype=dt;
            this.left=left;
            this.right=right;
            this.next=next;
        }
        /** Datatype object to validate. */
        public final Datatype datatype;
        /** Left and right states. */
        public final State.Single left,right;
        
        /** Next transition of this kind, or null. */
        public final Data next;
    }
    
    /** Transition by attribute. */
    public final static class Element {
        public Element( int mask, int test, State.Single left, State.Single right, Element next ) {
            this.mask = mask;
            this.test = test;
            this.left = left;
            this.right = right;
            this.next = next;
        }
        /** Name signature. */
        public final int mask,test;
        /** Left and right states. */
        public final State.Single left,right;
        
        /** Next transition of this kind, or null. */
        public final Element next;
        
        /** Returns true if this alphabet accets the given name code. */
        public boolean accepts( int nameCode ) {
            return (nameCode&mask)==test;
        }

        /** State that represents the head of the content model when no attribute is present. */
        private State noAttContentHead = null;
        
        protected State startElement( State result, AttributesSet attributes, StateFactory factory ) {
            // e.left is very likely to be expandable, so don't bother
            // use expandFast.
            State s1;
            
            if( attributes.size()==0 ) {
                if( noAttContentHead!=null )
                    s1 = noAttContentHead;
                else {
                    // create one. This needs to be synchronized because
                    // this object could be accessed simultaneously by many threads
                    synchronized(this) {
                        s1 = left.expand( attributes, State.emptySet, factory );
                        // the first if statement of "noAttContentHead!=null" is
                        // unsynchronized. There's a very small danger of two threads
                        // simultaneously entering the else clause.
                        if(noAttContentHead==null)
                            noAttContentHead = s1;
                    }
                }
            } else 
                s1 = left.expand( attributes, State.emptySet, factory );
            
            State s2 = factory.makeAfter( s1, right );
            
            return factory.makeChoice( result, s2 );
        }
    }
    
    /** Transition by interleave. */
    public final static class Interleave {
        public Interleave( State.Single left, State.Single right, State.Single join, boolean textToLeft, Interleave next ) {
            this.left = left;
            this.right = right;
            this.join = join;
            this.next = next;
            this.textToLeft = textToLeft;
        }
        /** Left and right states. */
        public final State.Single left,right;
        /** Join state. */
        public final State.Single join;
        
        /** True if text should be consumed by the left sub-automaton. */
        public final boolean textToLeft;
        
        /** Next transition of this kind, or null. */
        public final Interleave next;
    }

    /** Transition by list. */
    public final static class List {
        public List( State.Single left, State.Single right, List next ) {
            this.left = left;
            this.right = right;
            this.next = next;
        }
        /** Left and right states. */
        public final State.Single left,right;
        
        /** Next transition of this kind, or null. */
        public final List next;
    }

    /** Transition by non-existent attribute. */
    public final static class NoAtt {
        public NoAtt( State.Single right, int[] negTests, int[] posTests, NoAtt next ) {
            this.right = right;
            this.negTests = negTests;
            this.posTests = posTests;
            this.next = next;
        }
        /** Right state. */
        public final State.Single right;
        
        /** Name tests. repeated (mask,test) pairs. */
        private final int[] negTests, posTests;
        
        /** Next transition of this kind, or null. */
        public final NoAtt next;
        
        public boolean accepts( int code ) {
            for( int i=posTests.length-2; i>=0; i-=2 )
                if( (code&posTests[i])==posTests[i+1] )
                    return false;
            
            for( int i=negTests.length-2; i>=0; i-=2 )
                if( (code&negTests[i])==negTests[i+1] )
                    return true;
            
            return false;
        }
    }
    
    /** Transition by value. */
    public final static class Value {
        public Value( Datatype dt, Object value, State.Single right, Value next ) {
            this.datatype=dt;
            this.value = value;
            this.right=right;
            this.next=next;
        }
        /** Datatype object to validate. */
        private final Datatype datatype;
        /** Value object to compare with. */
        private final Object value;
        
        public boolean accepts( String text, ValidationContext context ) {
            Object o = datatype.createValue(text,context);
            if(o==null)     return false;
            return datatype.sameValue(o,value);
        }
        
        /** Left and right states. */
        public final State.Single right;
        
        /** Next transition of this kind, or null. */
        public final Value next;
    }

}
