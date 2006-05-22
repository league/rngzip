package org.kohsuke.bali.automaton.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.bali.automaton.NameSignature;

import com.sun.msv.grammar.AnyNameClass;
import com.sun.msv.grammar.AttributeExp;
import com.sun.msv.grammar.ChoiceNameClass;
import com.sun.msv.grammar.ElementExp;
import com.sun.msv.grammar.Grammar;
import com.sun.msv.grammar.ReferenceExp;
import com.sun.msv.grammar.NameClass;
import com.sun.msv.grammar.NamespaceNameClass;
import com.sun.msv.grammar.NotNameClass;
import com.sun.msv.grammar.SimpleNameClass;
import com.sun.msv.grammar.util.ExpressionWalker;
import com.sun.msv.grammar.util.NameClassCollisionChecker;
import com.sun.msv.util.StringPair;

/**
 * Assigns bit representation to each name
 * and signature to each name class.
 * 
 * <p>
 * NameClasses need to be unified before they are passed to this class.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NameClassEncoder {

    /** Computes name class encoding from a set of NameClasses. */
    public static NameClassEncoder build( NameClass[] names ) throws TooComplicatedException {
        NameClassEncoder nce = new NameClassEncoder(names);
        nce.doBuild();
        return nce;
    }

    /** Computes name class encoding from a set of NameClasses. */
    public static NameClassEncoder build( Collection names ) throws TooComplicatedException {
        NameClassEncoder nce = new NameClassEncoder(names);
        nce.doBuild();
        return nce;
    }
    
    /** Computes name class encoding from a grammar. */
    public static NameClassEncoder build( Grammar g ) throws TooComplicatedException {
        final Set names = new HashSet();
        
        // collect all name classes
        g.getTopLevel().visit(new ExpressionWalker() {
            public void onAttribute(AttributeExp exp) {
                if(visitedExps.add(exp)) {
                    names.add(exp.nameClass);
                    exp.exp.visit(this);
                }
            }
            public void onElement(ElementExp exp) {
                if(visitedExps.add(exp)) {
                    names.add(exp.getNameClass());
                    exp.contentModel.visit(this);
                }
            }
            public void onRef(ReferenceExp exp) {
                if(visitedExps.add(exp)) {
                    exp.exp.visit(this);
                }
            }
            private final Set visitedExps = new HashSet();
        });
        
        return build(names);
    }
    


    /** Map from NameClass to its Signature. */
    public final Map signatures = new HashMap();
    
    /** Name code for literals. Map from StringPair to Integer. */
    public final Map literals = new HashMap();
    
    /** number of bits that are used. */
    private int usedBits=0;
    public int getUsedBits() { return usedBits; }
    
    /** computed Clusters. */
    private Cluster[] clusters;
    
    /** used to build encoding but once it's built, this field becomes empty. */
    private final Set names = new HashSet();
    
    public final NameClassUnifier unifier = new NameClassUnifier();
    
    
    /**
     * A set of independet name classes that share the same bit mask.
     */
    private class Cluster {
        /** name classes in the ascending order of the test bits. */
        final NameClass[] nameClasses;
        /** True if the above set covers the possible names completely. */
        final boolean       isCover;
        /** Number of bits assigned for this cluster. */
        final int           bitLen;
        /** position of the least siginificant bit of this cluster in the whole bit masks. */
        final int           bitPos;
        
        Cluster( NameClass[] ncs, boolean cov, int bitLen, int bitPos ) {
            this.nameClasses = ncs;
            this.isCover = cov;
            this.bitLen = bitLen;
            this.bitPos = bitPos;
        }
    }



    private NameClassEncoder( Collection _names ) {
        for (Iterator itr = _names.iterator(); itr.hasNext();) {
            NameClass nc = (NameClass) itr.next();
            names.add( unifier.unify(nc) );
        }
    }
    private NameClassEncoder( NameClass[] _names ) {
        for( int i=0; i<_names.length; i++ )
            names.add( unifier.unify(_names[i]) ); // collision elimination
    }
    
    
    private void doBuild() throws TooComplicatedException {

        // collect probe points
        ProbePointsCollector ppc = new ProbePointsCollector();
        for( Iterator itr=names.iterator(); itr.hasNext(); )
            ((NameClass)itr.next()).visit(ppc);

        // list up simple names.
        List simpleNames = new ArrayList();
        for( Iterator itr = names.iterator(); itr.hasNext();) {
            NameClass nc = (NameClass) itr.next();
            if( nc instanceof SimpleNameClass )
                simpleNames.add(nc);
        }
        
        
        ArrayList clusters = new ArrayList();
        
        while( !names.isEmpty() ) {
            // find an independent set from names.
            ArrayList c = findIndependentSet();
            int count = c.size();
            boolean isCover = this.isCover(c);
            
            // if c is not a cover, we need one cluster that represents "others"
            if( !isCover )      count++;
            
            // compute the number of bits that need to represent this cluster.
            int nBits=log2(count);
            
            if( usedBits+nBits>32 )
                throw new TooComplicatedException();    // we don't have that many bits
            
            int mask = createMask(usedBits,nBits);
            
            // assign signatures
            for( int i=0; i<c.size(); i++ ) {
                NameClass nc = (NameClass)c.get(i);
                signatures.put( nc, new NameSignature(nc,mask,i<<usedBits,this) );
            }
            
            clusters.add(new Cluster(
                (NameClass[]) c.toArray(new NameClass[c.size()]),
                isCover, nBits, usedBits ));
            
            usedBits += nBits;
            
            // update names   
            names.removeAll(c);
            
            // repeat this process
        }
        
        this.clusters = (Cluster[]) clusters.toArray(new Cluster[clusters.size()]);


        // determine name code for name literals.
        for( Iterator itr=ppc.probePoints.iterator(); itr.hasNext(); ) {
            StringPair literal = (StringPair)itr.next();
            int code = getLiteralCode(literal);
            
            if(literal.localName!=IMPOSSIBLE) {
                if( getLiteralCode(new StringPair(literal.namespaceURI,IMPOSSIBLE))==code )
                    // this literal is of the form "foo:bar" and its encoded representation
                    // is the same as "foo:*". Thus there's no point in having this literal
                    continue;
            }
            else
            if(literal.namespaceURI!=IMPOSSIBLE) {
                if( getLiteralCode(new StringPair(IMPOSSIBLE,IMPOSSIBLE))==code )
                    // this literal is of the form "foo:*" and its encoded representation
                    // is the same as "*:*". Thus there's no point in having this literal
                    continue;
            }
            
            literals.put( literal, new Integer(code) );
        }
        
        // then update the name signature again so that
        // simple name classes will have -1 as the mask.
        for( int i=0; i<simpleNames.size(); i++ ) {
            SimpleNameClass snc = (SimpleNameClass)simpleNames.get(i);
            
            int code = getLiteralCode(snc.toStringPair());
            
            signatures.put( snc, new NameSignature(snc,-1,code,this) );
        }
        
    }

    /** Obtains the signature computed for the given name class. */
    public NameSignature getSignature( NameClass nc ) {
        NameSignature ns = (NameSignature)signatures.get(unifier.unify(nc));
        if(ns==null)
            throw new InternalError();  // algorithmic error
        return ns;
    }
    
    /**
     * Computes the encoded representation of a string literal.
     * 
     * A literal can contain wildcard in its parameter.
     */
    private int getLiteralCode( StringPair literal ) {
        int code=0;
        
        for( int i=this.clusters.length-1; i>=0; i-- ) {
            Cluster c = this.clusters[i];
            
            code <<= c.bitLen;
            
            int j=0;
            for( ; j<c.nameClasses.length; j++ )
                if( c.nameClasses[j].accepts(literal) ) {
                    code |= j;
                    break;
                }
            if(j==c.nameClasses.length)
                // no match. fill those bits by one
                code |= (1<<c.bitLen)-1;
        }
        return code;
    }
    
    
    /**
     * Computes ceil(log2(n)). The number of bits necessary
     * to represent <i>n</i> distinct things.
     */
    private int log2( int n ) {
        int nBits=0;
        
        n--;
        while(n!=0) {
            nBits++;
            n >>= 1;
        }
        return nBits;
    }
    
    private int createMask( int zeroLen, int oneLen ) {
        int mask=0;
        for( int i=0; i<oneLen; i++ )
            mask = (mask<<1)|1;
        
        return mask<<zeroLen;
    }

    
    
    
    
    /**
     * Finds an independet set from the "names" field.
     * 
     * Ideally, we'd like to compute the maximum independent set,
     * but that is a NP-complete problem.
     */
    private ArrayList findIndependentSet() {
        return findIndependentSet( new ArrayList() );
    }

    /**
     * Finds an independent set.
     * 
     * This function computes an independent set by starting with
     * all the simple name classes.
     * 
     * <p>
     * Since most of the name classes are {@link SimpleNameClass},
     * and they are independent to each other, this is usually
     * a good way to get a large independent set.
     * 
     * <p>
     * This algorithm can't be applied after the second iteration
     * because all the simple classes are wiped out in the first iteration.
     */
    private ArrayList findIndependentSet2() {
        ArrayList result = new ArrayList();
        
        for( Iterator itr = names.iterator(); itr.hasNext();) {
            NameClass nc = (NameClass)itr.next();
            
            if( nc instanceof SimpleNameClass )
                result.add(nc);
        }
        
        return findIndependentSet(result);
    }
    
    
    /**
     * Finds an independet set by extending the given set.
     * 
     * This function simply increases the given set in a greedily manner.
     * 
     * @param
     *      partial independent set.
     */
    private ArrayList findIndependentSet( ArrayList result ) {
        NameClassCollisionChecker checker = new NameClassCollisionChecker();
        
        for( Iterator itr = names.iterator(); itr.hasNext();) {
            NameClass nc = (NameClass)itr.next();
            
            // if a new name doesn't collide with existing ones, take it
            int i;
            for( i=0; i<result.size(); i++ )
                if( checker.check(nc,(NameClass)result.get(i)) )
                    break;  // collision
            
            if( i==result.size() )
                result.add(nc);
        }
        
        return result;
    }
    /**
     * Returns true if the parameters together covers the entire
     * name class space.
     * 
     * <p>
     * The formal definition is:
     * <pre>
     * isCover( nc1, nc2, ... nck )
     *    :=   ( choice(nc1,nc2,nck) == anyName )
     * </pre>
     */
    private boolean isCover( Collection nameClasses ) {
        if( nameClasses.size()==0 )     return false;
        
        // build the union of all the given name classes
        NameClass union=null;
        for( Iterator itr = nameClasses.iterator(); itr.hasNext(); ) {
            NameClass nc = (NameClass)itr.next();
            if(union==null)     union = nc;
            else                union = new ChoiceNameClass(union,nc);
        }
        
        // see if it has any "hole" in it --- 
        return new NotNameClass(union).isNull();
    }
    
    
    
    //
    //
    // debug functions
    //
    //
    
    
    /**
     * Dumps all the computed signatures.
     */
    public void dumpSigntures() {
        System.out.println("name classes");
        for( Iterator itr = signatures.entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry e = (Map.Entry) itr.next();
            
            System.out.println( e.getValue() );
        }
        System.out.println("\n\n");
        
        
        System.out.println("name literals");
        for( Iterator itr=literals.entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry e = (Map.Entry) itr.next();
            
            System.out.println( toBitString(((Integer)e.getValue()).intValue())+" : "+e.getKey() );
        }
    }
    
    private String toBitString( int n ) {
        String s="";
        for( int i=0; i<usedBits; i++ )
            if( (n&(1<<i))!=0 )     s='1'+s;
            else                    s='0'+s;
        return s;
    }
    
    
    // a very simple test
    public static void main( String[] args ) throws Exception {
        NameClass ns1 = new NamespaceNameClass("foo");
        NameClass ns2 = new NamespaceNameClass("bar");
        NameClass ns3 = new NamespaceNameClass("zot");
        
        NameClassEncoder e = NameClassEncoder.build(
            new NameClass[]{
                AnyNameClass.theInstance,
                new ChoiceNameClass(ns1,ns2),
                new ChoiceNameClass(ns2,ns3),
                new ChoiceNameClass(ns3,ns1),
                ns1,
                new SimpleNameClass("foo","zot")});
        
        e.dumpSigntures();
    }


    /** Invalid name token constant. */
    public static final String IMPOSSIBLE = "\u0000";
}
