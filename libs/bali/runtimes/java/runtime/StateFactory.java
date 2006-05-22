package runtime;

/**
 * COPYRIGHT WARNING: This code is taken from Jing!!!!!
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
final class StateFactory {

    private static final int INIT_SIZE = 255;
    private static final float LOAD_FACTOR = 0.3f;
    
    // memoization table. one instance per one type
    private final After after = new After();
    private final Choice choice = new Choice();
    private final Interleave interleave = new Interleave();


    State makeAfter( State child, State then ) {
        if( child==State.emptySet ) return child;
        if( then==State.emptySet )  return then;
        
        return after.create( child, then );
    }


    
    /**
     * Creates a new State.Choice object.
     * 
     * Caller needs to addRef two parameters.
     */
    State makeChoice( State block, State primitive ) {
        if(primitive instanceof State.Choice)
            throw new InternalError("primitive is also a choice");  // a bug in the algorithm
        
        if( block==State.emptySet )
            return primitive;
        if( primitive==State.emptySet )
            return block;
        if( block.contains(primitive) )
            return block;
        
        return choice.create(block,primitive);
    }
    
    
    /**
     * Creates a new Interleave object.
     * 
     * Caller needs to addRef two parameters.
     */
    State makeInterleave( State lhs, State rhs, Transition.Interleave alphabet ) {
        if(lhs==State.emptySet)     return lhs;
        if(rhs==State.emptySet)     return rhs;
        
        return interleave.create( lhs, rhs, alphabet );
    }






    
    
    /**
     * Common base class for three slightly different table implementations.
     */
    private abstract static class AbstractStateTable {
        protected int used =0;
        protected int usedLimit =(int) (INIT_SIZE * LOAD_FACTOR);
        protected int tableSize =INIT_SIZE;

    
        /** First-time hash function. */
        protected abstract int firstIndex(State s);
        
        /** Successive re-hash function. */
        protected final int nextIndex(int h) {
            return h==0 ? tableSize-1 : h-1;
        }
        
    
        
    
        /**
         * rehash contenst of oldTable into newTable.
         */
        protected final void rehash(State[] oldTable, State[] newTable) {
            
            for (int i = oldTable.length; i > 0;) {
                --i;
                if (oldTable[i] != null) {
                    int j;
                    for (j = firstIndex(oldTable[i]);
                        newTable[j] != null;
                        j = nextIndex(j));
                    newTable[j] = oldTable[i];
                }
            }
            usedLimit = (int) (newTable.length * LOAD_FACTOR);
            tableSize = newTable.length;
        }
    }
    
    /**
     * Table for hash-consing. This table is used only for
     * {@link State.After}.
     */
    private final static class After extends AbstractStateTable {
        private State.After[] table;
    
        After() {
            table = new State.After[INIT_SIZE];
        }
        
        private final int firstIndex( Object a, Object b ) {
            return ( (a.hashCode()^b.hashCode())&0x7FFFFFFF )%tableSize;
        }
        protected int firstIndex(State s) {
            State.After a = (State.After)s;
            return firstIndex( a.child, a.then );
        }
        
        State.After create(State child, State then) {
            int h;
    
            for (h = firstIndex(child,then); table[h] != null; h = nextIndex(h)) {
                State.After t = table[h];
                if( t.child==child && t.then==then )
                    return t;
            }
            
            if (used >= usedLimit) {
                State.After[] old = table;
                table = new State.After[ old.length<<1 ];
                rehash(old,table);
                
                for (h = firstIndex(child,then); table[h] != null; h = nextIndex(h))
                    ;
            }
            
            used++;
            return table[h] = new State.After(child,then);
        }
    }


    
    /**
     * Table for hash-consing. This table is used only for
     * {@link State.After}.
     */
    private final static class Choice extends AbstractStateTable {
        private State.Choice[] table;
    
        Choice() {
            table = new State.Choice[INIT_SIZE];
        }
        
        private final int firstIndex( Object a, Object b ) {
            return ( a.hashCode()^b.hashCode()&0x7FFFFFFF )%tableSize;
        }
        protected int firstIndex(State s) {
            State.Choice c = (State.Choice)s;
            return firstIndex( c.lhs, c.rhs );
        }
    
        State.Choice create(State lhs, State rhs) {
            int h;
    
            for (h = firstIndex(lhs,rhs); table[h] != null; h = nextIndex(h)) {
                State.Choice r = table[h];
                if( r.lhs==lhs && r.rhs==rhs )
                    return r;
            }
            
            if (used >= usedLimit) {
                State.Choice[] old = table;
                table = new State.Choice[ old.length<<1 ];
                rehash(old,table);
                
                for (h = firstIndex(lhs,rhs); table[h] != null; h = nextIndex(h))
                    ;
            }
            used++;
            return table[h] = new State.Choice(lhs,rhs);
        }
    }


    
    /**
     * Table for hash-consing. This table is used only for
     * {@link State.After}.
     */
    private final static class Interleave extends AbstractStateTable {
        private State.Interleave[] table;
    
        Interleave() {
            table = new State.Interleave[INIT_SIZE];
        }
        
        private final int firstIndex( Object a, Object b, Object c ) {
            return ( (a.hashCode()^b.hashCode()^c.hashCode())&0x7FFFFFFF )%tableSize;
        }
        protected int firstIndex(State s) {
            State.Interleave i = (State.Interleave)s;
            return firstIndex( i.lhs, i.rhs, i.alphabet );
        }
    
        State.Interleave create(State lhs, State rhs, Transition.Interleave alpha ) {
            int h;
    
            for (h = firstIndex(lhs,rhs,alpha); table[h] != null; h = nextIndex(h)) {
                State.Interleave r = table[h];
                if( r.lhs==lhs && r.rhs==rhs && r.alphabet==alpha )
                    return r;
            }
            
            if (used >= usedLimit) {
                State.Interleave[] old = table;
                table = new State.Interleave[ old.length<<1 ];
                rehash(old,table);
                
                for (h = firstIndex(lhs,rhs,alpha); table[h] != null; h = nextIndex(h))
                    ;
            }
            used++;
            return table[h] = new State.Interleave(lhs,rhs,alpha);
        }
    }
}
