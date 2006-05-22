package org.kohsuke.bali.automaton.builder;

/**
 * Thrown when the name classes can't be encoded in 32 bits.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class TooComplicatedException extends RuntimeException {
    // to make this exception usable inside visitor pattern,
    // this class needs to be a runtime exception.
}
