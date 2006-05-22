package org.kohsuke.bali.writer;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.kohsuke.bali.automaton.AlphabetVisitor;
import org.kohsuke.bali.automaton.AttributeAlphabet;
import org.kohsuke.bali.automaton.DataAlphabet;
import org.kohsuke.bali.automaton.ElementAlphabet;
import org.kohsuke.bali.automaton.InterleaveAlphabet;
import org.kohsuke.bali.automaton.ListAlphabet;
import org.kohsuke.bali.automaton.NameSignature;
import org.kohsuke.bali.automaton.NonExistentAttributeAlphabet;
import org.kohsuke.bali.automaton.State;
import org.kohsuke.bali.automaton.Transition;
import org.kohsuke.bali.automaton.TreeAutomaton;
import org.kohsuke.bali.automaton.ValueAlphabet;
import org.kohsuke.bali.automaton.builder.TooComplicatedException;
import org.kohsuke.bali.datatype.DatatypeImpl;
import org.kohsuke.bali.datatype.ValidationContextImpl;
import org.kohsuke.bali.datatype.Value;
import org.relaxng.datatype.Datatype;

import com.sun.msv.util.StringPair;

/**
 * Encodes an automaton into a set of strings so that
 * the Java/C# validatelet can process it.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class StringBasedEncoder implements AutomatonWriter {

    /**
     * Processes the encoded strings.
     */
    protected abstract void write(
        String encNameCodes,
        int defaultNameCode,
        String encStates,
        String encATr,
        String encDTr,
        String encETr,
        String encITr,
        String encLTr,
        String encNTr,
        String encVTr,
        DatatypeImpl[] datatypes,
        Value[] values
    ) throws IOException;
    
    
    public void write(TreeAutomaton automaton) throws IOException {
        
        StringBuffer encNameCodes = new StringBuffer();
        int defaultNameCode;
        {// encode name code map
            StringPair[] literals = automaton.listNameCodes();
            for( int i=0; i<literals.length; i++ ) {
                StringPair literal = literals[i];
                if( literal==TreeAutomaton.WILDCARD )   continue;   // don't need to encode this.
                
                encNameCodes.append( encodeInt(automaton.getNameCode(literal)) );
                encNameCodes.append(literal.namespaceURI.equals("\u0000")?"*":literal.namespaceURI);
                encNameCodes.append('\u0000');
                encNameCodes.append(literal.localName.equals("\u0000")?"*":literal.localName);
                encNameCodes.append('\u0000');
            }
            
            defaultNameCode = automaton.getNameCode(TreeAutomaton.WILDCARD);
        }



        
        // encode all the states into one string.
        State[] states = automaton.getStates();
        limit( states.length < 65535 ); // 0xFFFF is reserved for "not exist".
        
        StringBuffer encStates = new StringBuffer(states.length*8); // encoded states
        final StringBuffer encATr = new StringBuffer(); // encoded attribute transitions
        final StringBuffer encDTr = new StringBuffer(); // encoded data transitions
        final StringBuffer encETr = new StringBuffer(); // encoded element transitions
        final StringBuffer encITr = new StringBuffer(); // encoded interleave transitions
        final StringBuffer encLTr = new StringBuffer(); // encoded list transitions
        final StringBuffer encNTr = new StringBuffer(); // encoded non-existent attribute transitions
        final StringBuffer encVTr = new StringBuffer(); // encoded value transitions
        
        // datatypes that are already encoded
        final List datatypes = new ArrayList();
        final List values = new ArrayList();
        
        for( int i=0; i<states.length; i++ ) {
            State s = states[i];
            encStates.append((char)('0'
                +(s.getTextSensitivity()*4) // 2 bits
                +(s.isFinal?2:0)
                +(s.isPersistent()?1:0)));   // bit-encoded flags: isFinal/isPersistent/isTextIgnorable
            encStates.append(encode(s.nextState));
            encStates.append((char)encATr.length());  // index to the transition table
            encStates.append((char)encDTr.length());
            encStates.append((char)encETr.length());
            encStates.append((char)encITr.length());
            encStates.append((char)encLTr.length());
            encStates.append((char)encNTr.length());
            encStates.append((char)encVTr.length());
            
            Transition[] trs = s.getDeclaredTransitions();
            for( int j=0; j<trs.length; j++ ) {
                final Transition tr = trs[j];
                
                tr.alphabet.accept(new AlphabetVisitor() {
                    public Object attribute(AttributeAlphabet alpha) {
                        encATr.append(encode(tr.left));
                        encATr.append(encode(tr.right));
                        encATr.append(encode(alpha.name));
                        encATr.append(alpha.repeated?'R':'-');
                        return null;
                    }

                    public Object element(ElementAlphabet alpha) {
                        encETr.append(encode(tr.left));
                        encETr.append(encode(tr.right));
                        encETr.append(encode(alpha.name));
                        return null;
                    }

                    public Object nonExistentAttribute(NonExistentAttributeAlphabet alpha) {
                        limit( alpha.negativeNameTests.length<256 );
                        limit( alpha.positiveNameTests.length<256 );

                        // left state is unused with this type of alphabets
                        encNTr.append(encode(tr.right));
                        
                        // encode the size of name signatures
                        encNTr.append( (char)(
                            (alpha.negativeNameTests.length<<8)+alpha.positiveNameTests.length) );
                        
                        for( int i=0; i<alpha.negativeNameTests.length; i++ )
                            encNTr.append( encode(alpha.negativeNameTests[i]) );

                        for( int i=0; i<alpha.positiveNameTests.length; i++ )
                            encNTr.append( encode(alpha.positiveNameTests[i]) );
                        
                        return null;
                    }

                    public Object interleave(InterleaveAlphabet alpha) {
                        encITr.append(encode(tr.left));
                        encITr.append(encode(tr.right));
                        encITr.append(encode(alpha.join));
                        encITr.append(alpha.textToLeft?'L':'R');
                        
                        return null;
                    }

                    public Object list(ListAlphabet alpha) {
                        encLTr.append(encode(tr.left));
                        encLTr.append(encode(tr.right));
                        
                        return null;
                    }

                    public Object data(DataAlphabet alpha) {
                        encDTr.append(encode(tr.left));
                        encDTr.append(encode(tr.right));
                        encDTr.append(encode(alpha.datatype,datatypes));
                        
                        return null;
                    }

                    public Object value(ValueAlphabet alpha) {
                        // encode the value object and obtain its index
                        int idx = values.size();
                        values.add(alpha.value);
                        
                        encVTr.append(encode(tr.right));
                        encVTr.append(encode(alpha.datatype,datatypes));
                        encVTr.append((char)idx);
                        
                        return null;
                    }
                });
            }
        }
        
        limit( encATr.length()<Character.MAX_VALUE);
        limit( encDTr.length()<Character.MAX_VALUE);
        limit( encETr.length()<Character.MAX_VALUE);
        limit( encITr.length()<Character.MAX_VALUE);
        limit( encLTr.length()<Character.MAX_VALUE);
        limit( encNTr.length()<Character.MAX_VALUE);
        limit( encVTr.length()<Character.MAX_VALUE);
        
        
        write( encNameCodes.toString(), defaultNameCode, encStates.toString(),
            encATr.toString(),
            encDTr.toString(),
            encETr.toString(),
            encITr.toString(),
            encLTr.toString(),
            encNTr.toString(),
            encVTr.toString(),
            (DatatypeImpl[]) datatypes.toArray(new DatatypeImpl[datatypes.size()]),
            (Value[]) values.toArray(new Value[values.size()]) );
    }
    
    
    
    /** Encodes an integer (32 bits) into two charactres (2*16bits) */
    private String encodeInt( int i ) {
        return new String(new char[]{ (char)(i&0xFFFF), (char)(i>>16) });
    }
    
    /** Encodes a state into a character. */
    private char encode( State s ) {
        if(s==null)     return '\uFFFF';
        else            return (char)s.id;
    }
    
    /** Encodes a name signature into 4 characters. */
    private String encode( NameSignature name ) {
        return encodeInt(name.mask) + encodeInt(name.test);
    }
    
    /** Encodes a datatype object and returns its index. */
    private char encode( Datatype _dt, List datatypes ) {
        DatatypeImpl dt = (DatatypeImpl)_dt;
        // see if this datatype has already been encoded
        int idx = datatypes.indexOf(dt);
        if(idx==-1) {
            idx = datatypes.size();
            datatypes.add(dt);
        }
        
        return (char)idx;
    }

    /** Encodes a datatype object and returns its index. */
    protected String encode( DatatypeImpl dt ) {
        
        StringBuffer buf = new StringBuffer();
        for( int i=0; i<dt.parameters.length; i++ ) {
            if( buf.length()!=0 )
                buf.append(',');
            
            buf.append(escape(dt.parameters[i].name));
            buf.append(',');
            buf.append(escape(dt.parameters[i].value));
            buf.append(',');
            
            buf.append(encodeContext(dt.parameters[i].context));
        }
        
        return escape(dt.nsURI)+","
            +  escape(dt.name)+","
            + "new Object[]{"+buf+"}";
    }
    
    protected String encodeContext( ValidationContextImpl context ) {
        String[] contexts = context.getQueriedNamespaces();
        if( contexts.length==0 )
            return "null";
        else
            return "new String[]{"+flatten(contexts,',')+"}";
    }
    
    /**
     * Combines all strings in the collection by inserting a separator
     * between tokens.
     */
    private String flatten( String[] values, char sep ) {
        StringBuffer result = new StringBuffer();
        for( int i=0; i<values.length; i++ ) {
            if(result.length()!=0)  result.append(sep);
            result.append(escape(values[i]));
        }
        return result.toString();
    }
    
    /**
     * Escape unsafe characters in the target platform and
     * enquote.
     */
    protected abstract String escape( String str );
    
    
    /** Tests the limitation of this implementation. */
    private void limit( boolean test ) {
        if(!test)
            throw new TooComplicatedException();
    }
    
    
    
    
    
    
    //
    //
    // print with format
    //
    //
    
    protected String format( String propName, Object arg1 ) {
        return format( propName, new Object[]{arg1} );
    }
    protected String format( String propName, Object arg1, Object arg2 ) {
        return format( propName, new Object[]{arg1,arg2} );
    }
    protected String format( String propName, Object arg1, Object arg2, Object arg3 ) {
        return format( propName, new Object[]{arg1,arg2,arg3} );
    }
    
    protected String format( String property, Object[] args ) {
        String text = ResourceBundle.getBundle(
            this.getClass().getName()).getString(property);
        return MessageFormat.format(text,args);
    }
}
