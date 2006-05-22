package org.kohsuke.bali.writer;

import java.io.IOException;

import org.kohsuke.bali.automaton.TreeAutomaton;

/**
 * AutomatonWriter that produces nothing.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NullWriter implements AutomatonWriter {

    public void write(TreeAutomaton automaton) throws IOException {
        // noop
    }

}
