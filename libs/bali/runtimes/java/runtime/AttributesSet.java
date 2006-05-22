package runtime;
import org.relaxng.datatype.ValidationContext;
import org.xml.sax.Attributes;

/**
 * Set of attributes and their values.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class AttributesSet {
    
    /** Number of attributes in this set. */
    private int size =0;
    /** Encoded names of attributes. */
    private int[] names = new int[16];
    /** Values of attributes. */
    private String[] values = new String[16];
    /** Context object, in which datatypes evaluate values. */
    private ValidationContext context;
    
    private final static int CACHE_SIZE = 37;
    /**
     * States that are known to be valid wrt the attribute set.
     * Attribute values are often evaluated multiple times against
     * the same state, so this works as a cache to improve performance
     * in those cases.
     * 
     * <p>
     * This is used as a hashtable from the hash code.
     */
    private Transition.Att[] transitionCache = emptyCache;
    
    private final Transition.Att[] privateCache = new Transition.Att[CACHE_SIZE];
    
    private static final Transition.Att[] emptyCache = new Transition.Att[CACHE_SIZE];


    public AttributesSet() {}
    
    /** Re-initializes attributes. */
    public void reset( ValidateletImpl validatelet, Attributes atts, ValidationContext _context ) {
        size = atts.getLength();
        
        if( names.length<size ) {
            // buffer to small. reallocate the buffer
            names = new int[size];
            values = new String[size];
        }
        
        for( int i=size-1; i>=0; i-- ) {
            NameCodeMap.Entry e;
            
            names[i] = validatelet.getNameCode( atts.getURI(i), atts.getLocalName(i) ).nameCode;
            values[i] = atts.getValue(i);
        }
        
        this.transitionCache = emptyCache;
        this.context = _context;
    }
    
    public int size() { return size; }
//    public ValidationContext getContext() { return context; }
    
    public int getName( int idx ) { return names[idx]; }
    public String getValue( int idx ) { return values[idx]; }
    
    /**
     * Returns true if the attribute transition can be taken
     * with respect to this attribute set.
     */
    public boolean matchs( Transition.Att aa, StateFactory factory ) {
        int matchCount=0,failCount=0;
        
        // check the cache table
        if( transitionCache[ (aa.hashCode()&0x7FFFFFFF)%CACHE_SIZE ]==aa )
            return true;        // cache hit
        
        for( int j=size-1; j>=0; j-- ) {
            
            if( aa.accepts(names[j]) ) {
                String value = values[j];
                
                State s = aa.left.text(
                    value, value.trim().length()==0,
                    context, empty,
                    State.emptySet, factory );
                
                if( s.isFinal() )   matchCount++;
                else                failCount++;
            }
        }
        
        if( (matchCount==1 && failCount==0 && !aa.repeated)
        ||  (matchCount!=0 && failCount==0 && aa.repeated) ) {
            if( transitionCache==emptyCache ) {
                // reset the transition cache
                transitionCache = privateCache;
                System.arraycopy(emptyCache,0,privateCache,0,CACHE_SIZE);
    //        for( int i=CACHE_SIZE-1; i>=0; i-- )
    //            transitionCache[i] = null;
            }
            transitionCache[ (aa.hashCode()&0x7FFFFFFF)%CACHE_SIZE ] = aa;    // update the cache entry
            return true;
        } else
            return false;
    }
    
    
    /** Empty attributes available as a constant. */
    public final static AttributesSet empty = new AttributesSet();
    
}
