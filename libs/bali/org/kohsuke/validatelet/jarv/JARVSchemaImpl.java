package org.kohsuke.validatelet.jarv;

import org.iso_relax.verifier.Schema;
import org.iso_relax.verifier.Verifier;
import org.iso_relax.verifier.VerifierConfigurationException;

/**
 * Wraps a compiled {@link runtime.Schema} object into the JARV
 * {@link Schema} interface.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JARVSchemaImpl implements Schema {
    
    /**
     * 
     * @param
     *      Schema object being wrapped.
     */
    public JARVSchemaImpl( runtime.Schema schema ) {
        this.schema = schema;
    }

    private final runtime.Schema schema;
    
    public Verifier newVerifier() throws VerifierConfigurationException {
        return new JARVVerifierImpl(schema.createValidatelet());
    }

}
