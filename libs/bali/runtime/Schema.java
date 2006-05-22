package runtime;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.kohsuke.validatelet.Validatelet;
import org.kohsuke.validatelet.ValidateletFactory;
import org.relaxng.datatype.Datatype;
import org.relaxng.datatype.DatatypeBuilder;
import org.relaxng.datatype.DatatypeException;
import org.relaxng.datatype.DatatypeLibrary;
import org.relaxng.datatype.DatatypeLibraryFactory;
import org.relaxng.datatype.ValidationContext;

/**
 * Compiled schema.
 * 
 * <p>
 * Most of the code is simply to decode a state machine
 * from its string-encoded format. The only meaningful
 * data after the decoding is the initial state and
 * name literals.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class Schema implements ValidateletFactory {

    /** Initial state. */
    public final State.Single initialState;
    
    public Validatelet createValidatelet() {
        return new ValidateletImpl(this);
    }
    
//
//
// decoding
//
//
    private static final int sizeOfState = 9;
    private static final int sizeOfATr = 7; /* left(1),right(1),name(4),flag(1) */
    private static final int sizeOfDTr = 3; /* left(1),right(1),datatype(1) */
    private static final int sizeOfETr = 6; /* left(1),right(1),name(4) */
    private static final int sizeOfITr = 4; /* left(1),right(1),join(1),flag(1) */
    private static final int sizeOfLTr = 2; /* left(1),right(1) */
    // NTr is of variable length. The format is
    // right(1), negLen|posLen(1), name(4*negLen), name(4*posLen)
    private static final int sizeOfVTr = 3; /* right(1),datatype(1),value(1) */


    public Schema(
        String nameLiterals,
        int defaultNameCode,
        final String encStates,
        final String encATr,
        final String encDTr,
        final String encETr,
        final String encITr,
        final String encLTr,
        final String encNTr,
        final String encVTr,
        final Object[] encDatatypes,
        final Object[] values,
        final DatatypeLibraryFactory datatypeFactory ) {
        
        this( nameLiterals, defaultNameCode, encStates,
            encATr, encDTr, encETr, encITr, encLTr, encNTr, encVTr,
            createDatatypes( datatypeFactory, encDatatypes ),
            values );
    }
    
    /**
     * @param nameLiterals
     *      Name literals encoded into a string as
     *      "[stateId1][uri1]\u0000[local1]\u0000[stateid2][uri2]\u0000 ..."
     * @param defaultNameCode
     *      The name code assigned to literals that are not described in the above
     *      dictionary.
     * @param encStates
     *      encoded state information.
     * @param encATr, encDTr, encETr, encITr, encLTr, encNTr, encVTr
     *      encoded transition tables (per alphabet type.)
     * @param datatypes
     *      Datatype objects used in this schema.
     * @param values
     *      Values used by &lt;value/> patterns.
     */
    public Schema(
        String nameLiterals,
        int _defaultNameCode,
        final String encStates,
        final String encATr,
        final String encDTr,
        final String encETr,
        final String encITr,
        final String encLTr,
        final String encNTr,
        final String encVTr,
        final Datatype[] datatypes,
        final Object[] values ) {
        
        HashMap nameMap = new HashMap();

        // decode name literals
        while(nameLiterals.length()!=0) {
            int code = decodeInt(nameLiterals,0);   // name code
            nameLiterals = nameLiterals.substring(2);
            
            int idx;
            
            idx = nameLiterals.indexOf('\u0000');
            String uri = nameLiterals.substring(0,idx);
            nameLiterals = nameLiterals.substring(idx+1);
            
            idx = nameLiterals.indexOf('\u0000');
            String local = nameLiterals.substring(0,idx);
            nameLiterals = nameLiterals.substring(idx+1);
            
            nameMap.put( new StringPair(uri,local), new Integer(code) );
        }
        
        this.nameLiterals = Collections.unmodifiableMap(nameMap);
        
        
        this.defaultNameCode = _defaultNameCode;
        
        {// decode state and transition table
            String es;
            State.Single[] states = new State.Single[ encStates.length()/sizeOfState ];
            
            // build basic state objects
            es=encStates;            
            for( int idx=0; es.length()!=0; idx++,es=es.substring(sizeOfState) ) {
                char bitFlags = es.charAt(0);
                states[idx] = new State.Single(
                    (bitFlags&12/*1100B*/)>>2, (bitFlags&2)!=0, (bitFlags&1)!=0, idx );
            }
            
            boolean[] decoded = new boolean[states.length];
            
            // bare stack.
            int[] stack = new int[16];
            int stackPtr=0;
            
            // decode transition table
            for( int idx=states.length-1; idx>=0; idx-- ) {
                
                int s = idx;
                while( s!=65535 && !decoded[s] ) {
                    // this state needs to be decoded -- push this state
                    if( stack.length==stackPtr ) {
                        // extend the stack
                        int[] newBuf = new int[stack.length*2];
                        System.arraycopy(stack,0,newBuf,0,stack.length);
                        stack = newBuf;
                    }
                    stack[stackPtr++] = s;
                    decoded[s] = true;
                    
                    // decode next state
                    s = encStates.charAt( s*sizeOfState+1 );
                }
                
                while( stackPtr!=0 ) {
                    // decode transitions from state 's'.
                    s = stack[--stackPtr];
                    final State.Single current = states[s];
                    
                    // next state
                    final int nextStateIdx = encStates.charAt( s*sizeOfState+1 );
                    final State.Single nextState = (nextStateIdx==65535)?null:states[nextStateIdx];
                    
                    // decode transitions
                    
                    current.aTr = decodeATr( encStates, encATr, states, s, nextState==null?null:nextState.aTr );
                    current.dTr = decodeDTr( encStates, encDTr, states, s, nextState==null?null:nextState.dTr, datatypes );
                    current.eTr = decodeETr( encStates, encETr, states, s, nextState==null?null:nextState.eTr );
                    current.iTr = decodeITr( encStates, encITr, states, s, nextState==null?null:nextState.iTr );
                    current.lTr = decodeLTr( encStates, encLTr, states, s, nextState==null?null:nextState.lTr );
                    current.nTr = decodeNTr( encStates, encNTr, states, s, nextState==null?null:nextState.nTr );
                    current.vTr = decodeVTr( encStates, encVTr, states, s, nextState==null?null:nextState.vTr, datatypes, values );
                    
                    if( current.aTr!=null || current.iTr!=null || current.nTr!=null )
                        current.isExpandable = true;
                }
            }
            
            
            // build quick look-up table for elements
            for( int idx=states.length-1; idx>=0; idx-- ) {
                State.Single current = states[idx];
                
                TreeMap quicks = new TreeMap();
                ArrayList others = new ArrayList();
                
                for( Transition.Element e = current.eTr; e!=null; e = e.next ) {
                    if( e.mask==-1 ) {
                        Integer key = new Integer(e.test);
                        Transition.Element old = (Transition.Element)quicks.get(key);
                        Transition.Element copy = new Transition.Element(
                            e.mask, e.test, e.left, e.right, old );
                        quicks.put( key, copy );
                    } else
                        others.add(e);
                }
                
                // recreate elements
                Transition.Element e = null;
                for( int i=0; i<others.size(); i++ ) {
                    Transition.Element orig = (Transition.Element)others.get(i);
                    e = new Transition.Element(
                        orig.mask, orig.test, orig.left, orig.right, e );
                }
                current.eTr = e;
                
//                        current.quickETr = quicks;

                // create quick look-up map
                current.quickETr = new Transition.Element[quicks.size()];
                int i=0;
                for( Iterator itr=quicks.values().iterator(); itr.hasNext(); i++ )
                    current.quickETr[i] = (Transition.Element)itr.next();
            }
            
            initialState = states[0];
        }
    }

    /** Decodes attribute transitions. */
    private static Transition.Att decodeATr( String encStates, String encATr, State.Single[] states, int s, Transition.Att next ) {
        int start = encStates.charAt( s*sizeOfState+2 );
        int end = (s!=states.length-1)?encStates.charAt( (s+1)*sizeOfState+2 ):encATr.length();
        
        for( int i=end-sizeOfATr; i>=start; i-=sizeOfATr ) {
            next = new Transition.Att(
                decodeInt(encATr, i+2),
                decodeInt(encATr, i+4),
                encATr.charAt(i+6)=='R',
                states[encATr.charAt(i+0)],
                states[encATr.charAt(i+1)],
                next );
        }
        
        return next;
    }

    /** Decodes data transitions. */
    private static Transition.Data decodeDTr( String encStates, String encDTr, State.Single[] states, int s, Transition.Data next, Datatype[] datatypes ) {
        int start = encStates.charAt( s*sizeOfState+3 );
        int end = (s!=states.length-1)?encStates.charAt( (s+1)*sizeOfState+3 ):encDTr.length();
        
        for( int i=end-sizeOfDTr; i>=start; i-=sizeOfDTr ) {
            next = new Transition.Data(
                datatypes[ encDTr.charAt(i+2) ],
                states[encDTr.charAt(i+0)],
                states[encDTr.charAt(i+1)],
                next );
        }
        
        return next;
    }

    /** Decodes element transitions. */
    private static Transition.Element decodeETr( String encStates, String encETr, State.Single[] states, int s, Transition.Element next ) {
        int start = encStates.charAt( s*sizeOfState+4 );
        int end = (s!=states.length-1)?encStates.charAt( (s+1)*sizeOfState+4 ):encETr.length();
        
        for( int i=end-sizeOfETr; i>=start; i-=sizeOfETr ) {
            next = new Transition.Element(
                decodeInt(encETr, i+2),
                decodeInt(encETr, i+4),
                states[encETr.charAt(i+0)],
                states[encETr.charAt(i+1)],
                next );
        }
        
        return next;
    }
                    
    /** Decodes interleave transitions. */
    private static Transition.Interleave decodeITr( String encStates, String encITr, State.Single[] states, int s, Transition.Interleave next ) {
        int start = encStates.charAt( s*sizeOfState+5 );
        int end = (s!=states.length-1)?encStates.charAt( (s+1)*sizeOfState+5 ):encITr.length();
        
        for( int i=end-sizeOfITr; i>=start; i-=sizeOfITr ) {
            next = new Transition.Interleave(
                states[encITr.charAt(i+0)],
                states[encITr.charAt(i+1)],
                states[encITr.charAt(i+2)],
                encITr.charAt(i+3)=='L',
                next );
        }
        
        return next;
    }
                    
                    
    /** Decodes list transitions. */
    private static Transition.List decodeLTr( String encStates, String encLTr, State.Single[] states, int s, Transition.List next ) {
        int start = encStates.charAt( s*sizeOfState+6 );
        int end = (s!=states.length-1)?encStates.charAt( (s+1)*sizeOfState+6 ):encLTr.length();
        
        for( int i=end-sizeOfLTr; i>=start; i-=sizeOfLTr ) {
            next = new Transition.List(
                states[encLTr.charAt(i+0)],
                states[encLTr.charAt(i+1)],
                next );
        }
        
        return next;
    }

    /** Decodes non-existent attribute transitions. */
    private static Transition.NoAtt decodeNTr( String encStates, String encNTr, State.Single[] states, int s, Transition.NoAtt next ) {
        int start = encStates.charAt( s*sizeOfState+7 );
        int end = (s!=states.length-1)?encStates.charAt( (s+1)*sizeOfState+7 ):encNTr.length();
        
        for( int i=start; i<end; ) {
            State.Single right = states[encNTr.charAt(i+0)];
            char sz = encNTr.charAt(i+1);
            int szNeg = (sz>>8);
            int szPos = (sz&0xFF);
            
            int[] negTest = new int[szNeg*2];
            int[] posTest = new int[szPos*2];
            
            i+=2;
            
            for( int j=0; szNeg>0; szNeg--,j+=2,i+=4 ) {
                negTest[j+0] = decodeInt(encNTr,i+0);
                negTest[j+1] = decodeInt(encNTr,i+2);
            }
            for( int j=0; szPos>0; szPos--,j+=2,i+=4 ) {
                posTest[j+0] = decodeInt(encNTr,i+0);
                posTest[j+1] = decodeInt(encNTr,i+2);
            }
            
            next = new Transition.NoAtt(right,negTest,posTest,next);
        }
        
        return next;
    }

                    
    /** Decodes value transitions. */
    private static Transition.Value decodeVTr( String encStates, String encVTr, State.Single[] states, int s, Transition.Value next, Datatype[] datatypes, Object[] values ) {
        int start = encStates.charAt( s*sizeOfState+8 );
        int end = (s!=states.length-1)?encStates.charAt( (s+1)*sizeOfState+8 ):encVTr.length();
        
        for( int i=end-sizeOfVTr; i>=start; i-=sizeOfVTr ) {
            Datatype dt = datatypes[ encVTr.charAt(i+1) ];
            int vidx = encVTr.charAt(i+2)*2;
            next = new Transition.Value(
                dt,
                dt.createValue( (String)values[vidx], createContext((String[])values[vidx+1]) ),
                states[encVTr.charAt(i+0)],
                next );
        }
        
        return next;
    }



    




    
//
//
// name look-up
//
//
    /**
     * Dictionary for looking up name codes from (uri,local).
     * A map from StringPair to Integer. Unmodifiable.
     */
    public final Map nameLiterals;
    
    /** Default name code if a name is not found in the dictionary. */
    public final int defaultNameCode;
    
    /**
     * Immutable (URI,local name) pair. 
     */
    static final class StringPair {
        public final String uri;
        public final String local;
        
        StringPair( String uri, String local ) {
            this.uri=uri;
            this.local=local;
        }
        
        public final boolean equals( Object o ) {
            StringPair rhs = (StringPair)o;
            return this.uri.equals(rhs.uri) && this.local.equals(rhs.local);
        }
        
        public final int hashCode() {
            return uri.hashCode() ^ local.hashCode();
        }
    }

    /**
     * Looks up a name code from an (uri,local) pair.
     * 
     * @param name
     *      mutable StringPair object that represents the name.
     *      The content of this variable will be modified during this method.
     */
    public final int getNameCode( String uri, String local ) {
        Object o;
        
        o = nameLiterals.get(new StringPair(uri,local));
        if(o!=null)     return ((Integer)o).intValue();
        
        o = nameLiterals.get(new StringPair(uri,WILDCARD));
        if(o!=null)     return ((Integer)o).intValue();
        
        return defaultNameCode;
    }
        
    public static final String WILDCARD = "*";
    
  
    
//
//
// decode utility methods
//
//
    private static final int decodeInt( String s, int idx ) {
        return (((int)s.charAt(idx+1))<<16) | ((int)s.charAt(idx+0));
    }
    
    private static Datatype[] createDatatypes( DatatypeLibraryFactory factory, Object[] encodedDatatypes ) {
        Datatype[] datatypes = new Datatype[encodedDatatypes.length/3];
        for( int i=0; i<datatypes.length; i++ ) {
            datatypes[i] = createDatatype( factory,
                (String)encodedDatatypes[i*3],
                (String)encodedDatatypes[i*3+1],
                (Object[])encodedDatatypes[i*3+2] );
        }
        return datatypes;
    }
    
    private static Datatype createDatatype( DatatypeLibraryFactory factory,
        String nsUri, String localName, Object[] params ) {
        
        try {
            if( nsUri.length()==0 ) {
                // since those parameters were compiled, we don't need to check the error.
                if( localName.charAt(0)=='t' )      return BuiltinDatatypeLibrary.TOKEN;
                else                                return BuiltinDatatypeLibrary.STRING;
            }
            
            DatatypeLibrary lib = factory.createDatatypeLibrary(nsUri);
            if(lib==null)
                throw new DatatypeException("unable to locate a datatype library for "+nsUri);
            
            DatatypeBuilder builder = lib.createDatatypeBuilder(localName);
            
            for( int i=0; i<params.length; i+=3 ) {
                ValidationContext context = createContext( (String[])params[i+2] );
                
                builder.addParameter(
                    (String)params[i],
                    (String)params[i+1],
                    context );
            }
            
            return builder.createDatatype();
        } catch( DatatypeException e ) {
            e.printStackTrace();
            throw new InternalError(e.toString());
        }
    }
    
    private static ValidationContext createContext( final String[] map ) {
        if(map==null)   return null;
        
        return new ValidationContext() {
            public String resolveNamespacePrefix(String prefix) {
                for( int i=0; i<map.length/2; i++ )
                    if( map[i].equals(prefix) )
                        return map[i+1];
                return null;
            }
            public String getBaseUri() {
                return null;
            }
            public boolean isUnparsedEntity(String value) {
                return true;
            }
            public boolean isNotation(String value) {
                return true;
            }
        };
    }
    
    /**
     * Decodes GZip-encoded string to another string.
     * 
     * <p>
     * Java class file cannot have strings longer than 64K,
     * but big schemas sometimes exceed this limitation.
     * 
     * Therefore we will compress long string literals if it
     * exceeds this limit. This method decompress the compressed
     * string.
     */
    protected static String decompress( String enc ) {
        try {
            Reader r = new InputStreamReader(
                new GZIPInputStream(new StringInputStream(enc)),"UTF-8");
            
            StringBuffer result = new StringBuffer();
            char[] buf = new char[256];
            int l;
            while((l=r.read(buf))!=-1)
                result.append(buf,0,l);
            
            r.close();
            
            return result.toString();
        } catch( IOException e ) {
            e.printStackTrace();
            throw new InternalError();  // impossible
        }
    }
    
    private static class StringInputStream extends InputStream
    {
        StringInputStream( String str ) { s=str; }
        
        private String s;
        private int idx=0;
        
        public int read() throws IOException {
            if( idx==s.length() )   return -1;
            return s.charAt(idx++);
        }
    }
}
