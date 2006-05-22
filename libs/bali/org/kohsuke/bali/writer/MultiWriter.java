package org.kohsuke.bali.writer;

import java.io.IOException;

import org.kohsuke.bali.automaton.TreeAutomaton;

/**
 * Writes an automaton in two ways.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class MultiWriter implements AutomatonWriter {
    public MultiWriter( AutomatonWriter w1, AutomatonWriter w2 ) {
        this.w1 = w1;
        this.w2 = w2;
    }
    
    private final AutomatonWriter w1,w2;

    public void write(TreeAutomaton automaton) throws IOException {
        w1.write(automaton);
        w2.write(automaton);
    }
}
