package runtime;
import org.relaxng.datatype.Datatype;
import org.relaxng.datatype.DatatypeException;
import org.relaxng.datatype.DatatypeStreamingValidator;
import org.relaxng.datatype.ValidationContext;
import org.relaxng.datatype.helpers.StreamingValidatorImpl;

/**
 * RELAX NG built-in datatype library.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class BuiltinDatatypeLibrary {
    private BuiltinDatatypeLibrary() {}
    
    private static abstract class BaseImpl implements Datatype {
        public final void checkValid(String exp, ValidationContext context ) {}
        public final DatatypeStreamingValidator createStreamingValidator(ValidationContext context) {
            return new StreamingValidatorImpl(this,context);
        }
        public final int getIdType() { return 0; }
        public final boolean isContextDependent() { return false; }
        public final boolean isValid(String str, ValidationContext context) { return true; }
        public final boolean sameValue(Object lhs, Object rhs) {
            return lhs.equals(rhs);
        }
        public final int valueHashCode(Object o) { return o.hashCode(); }
    }
    
    /** String type, which works exactly the same as XML Schema string type. */
    public static final Datatype STRING = new BaseImpl() {
        public Object createValue(String str, ValidationContext context) {
            return str;
        }
    };
    
    /** Token type, which works exactly the same as XML Schema token type. */
    public static final Datatype TOKEN = new BaseImpl() {
        public Object createValue(String str, ValidationContext context) {
            StringBuffer buf = new StringBuffer();
            boolean inWhitespace = true;
            int len = str.length();
            
            for( int i=0; i<len; i++ ) {
                char ch = str.charAt(i);
                if( " \t\r\n".indexOf(ch)==-1 ) {
                    buf.append(ch);
                    inWhitespace = false;
                } else {
                    if(!inWhitespace)
                        buf.append(' ');
                    inWhitespace = true;
                }
            }
            
            if(inWhitespace && buf.length()!=0)
                buf.setLength(buf.length()-1);
            
            return buf.toString();
        }
    };
}
