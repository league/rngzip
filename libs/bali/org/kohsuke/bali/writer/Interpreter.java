package org.kohsuke.bali.writer;

import java.io.IOException;
import org.kohsuke.bali.datatype.DatatypeImpl;
import org.kohsuke.bali.datatype.Value;
import org.relaxng.datatype.Datatype;
import org.relaxng.datatype.DatatypeException;

import runtime.Schema;
import runtime.ValidateletImpl;

/**
 * Creates an instance of Schema object in the
 * current Java VM.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Interpreter extends StringBasedEncoder {

    protected void write(
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
        Value[] values)
        throws IOException {
        
        Datatype[] realDatatypes = new Datatype[datatypes.length];
        for( int i=0; i<datatypes.length; i++ )
            realDatatypes[i] = datatypes[i].realDatatype;
        
        Object[] valueParams = new Object[values.length*2];
        for( int i=0; i<values.length; i++ ) {
            valueParams[i*2+0] = values[i].value;
            valueParams[i*2+1] = values[i].context.getQueriedNamespaces();
        }
        
        schema = new Schema(
            encNameCodes, defaultNameCode, encStates,
            encATr, encDTr, encETr, encITr, encLTr, encNTr, encVTr,
            realDatatypes, valueParams );
    }

    private Schema schema;
    
    /**
     * Creates a new instance of Validatelet.
     */
    public ValidateletImpl createValidatelet() {
        return new ValidateletImpl(schema);
    }

    protected String escape(String str) {
        return Util.toJavaString(str);
    }
}
