package org.kohsuke.bali.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.MessageFormat;

import org.kohsuke.bali.automaton.ElementAlphabet;
import org.kohsuke.bali.automaton.State;
import org.kohsuke.bali.automaton.Transition;
import org.kohsuke.bali.automaton.TreeAutomaton;

import com.sun.msv.grammar.util.ExpressionPrinter;

/**
 * Dumps the automaton into a text file
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AutomatonDumper implements AutomatonWriter {
    
    private final PrintWriter out;
    
    
    public AutomatonDumper( OutputStream os ) {
        out = new PrintWriter(os);
    }
    
    public void write(TreeAutomaton automaton) throws IOException {
        System.err.println("dumping");
        
        State[] states = automaton.getStates();
        for( int i=0; i<states.length; i++ ) {
            State s = states[i];
            
            // print the name of state
            out.println("##"+Integer.toString(s.id)+"\t"+
                ExpressionPrinter.printContentModel(s.exp));
            
            Transition[] trans = s.getDeclaredTransitions();
            for( int j=0; j<trans.length; j++ ) {
                Transition t = trans[j];
                
                String left;
                if(t.left!=null)
                    left = Integer.toString(t.left.id);
                else
                    left = "(null)";
                
                out.println(MessageFormat.format("  {0} -> #{1} x #{2}",
                    new Object[]{ t.alphabet,
                        left,
                        Integer.toString(t.right.id) }));
            }
            
            if( s.nextState!=null ) {
                out.println("  -> #"+Integer.toString(s.nextState.id) );
            }
        }
        out.flush();
    }

}
