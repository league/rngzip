package org.kohsuke.validatelet;

/**
 * Factory that creates instances of {@link Validatelet}
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface ValidateletFactory {
    /**
     * Creates a new instance of Validatelet.
     */
    Validatelet createValidatelet();
}
