package org.kohsuke.bali.writer;

import java.io.IOException;

import org.kohsuke.bali.automaton.TreeAutomaton;

/**
 * An interface for those classes that can "print"
 * an automaton into another format, such as C source code
 * or a gif file.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface AutomatonWriter {
    
    /**
     * Writes the given automaton in another format.
     */
    public void write( TreeAutomaton automaton ) throws IOException;
}
