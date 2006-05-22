package org.kohsuke.bali.writer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;

import org.kohsuke.bali.automaton.*;

/**
 * Produces a gif/ps file from automaton by using
 * <a href="http://www.research.att.com/sw/tools/graphviz/">GraphViz</a>.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AutomatonVisualizer implements AutomatonWriter, AlphabetVisitor {
    public AutomatonVisualizer( String fileType, OutputStream target ) {
        this.fileType = fileType;
        this.target = target;
    }
    
    /** Format of the picture (e.g., "gif", "ps", "png".) */
    private final String fileType;
    
    /** Stream that receives the image. */
    private final OutputStream target;
    
    public void write( TreeAutomaton automaton ) throws IOException {
        System.err.println("producing a "+fileType+" file from the automaton");
        
        if( fileType.equals("dot") ) {
            out = new PrintWriter(target);
            writeDot(automaton);
        } else {
            Process proc = Runtime.getRuntime().exec(
                new String[]{"dot","-T"+fileType});
            out = new PrintWriter(
                new BufferedOutputStream(proc.getOutputStream()));

            writeDot(automaton);

            InputStream in = proc.getInputStream();
            byte[] buf = new byte[256];
            while(true) {
                int len = in.read(buf);
                if(len==-1)     break;
                target.write(buf,0,len);
            }
            in.close();
            
            try {
                proc.waitFor();
            } catch( InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }
    
    private void writeDot( TreeAutomaton automaton ) {
        out.println("digraph G {");
        out.println("node [shape=\"circle\"];");
        
        
        State[] states = (State[]) automaton.getStates();
        
        for( int i=0; i<states.length; i++ ) {
            s = states[i];  // to access this variable from visitor methods, 's' is a member variable
            
            String style="";
            
            if(s.isFinal && !s.isEpsilon())
                style += ",shape=\"doublecircle\"";

            if(s.getTextSensitivity()==State.TEXT_IGNORABLE)
                style += ",style=filled,fillcolor=lightgray";
            
            if(style.length()>0)
                out.println(s+" ["+style.substring(1)+"];");
            
            if(s.nextState!=null) {
                out.println(MessageFormat.format(
                    "{0} -> {1} [style=dotted]",
                    new Object[]{ s, s.nextState }));
            }
            
            Transition[] transitions = s.getDeclaredTransitions();
            for( int j=0; j<transitions.length; j++ ) {
                t = transitions[j];   // to access this variable from visitor methods, 't' is a member variable
                
                t.alphabet.accept(this);
            }
        }
        
        out.println("}");
        out.flush();
        out.close();
        
    }


    /**
     * OutputStream that is connected to stdin of dot.
     * Available only during the write method is being executed.
     */
    private PrintWriter out;
    
    /**
     * The current state.
     * We are drawing transitions leaving from this state.
     * Available only during the write method is being executed.
     */
    private State s;
    
    /**
     * The current transition.
     * We are drawing this transition as an edge.
     */
    private Transition t;
    


    /**
     * Writes a state as a circle and returns an identifier for it.
     */
    private String state( State s ) {
        if(s.isEpsilon()) {
            String tmp = getID();
            out.println(tmp+" [shape=\"doublecircle\",label=\"eps\"];");
            return tmp;
        } else {
            return s.toString();
        }
    }
    
    public Object attribute(AttributeAlphabet alpha) {
        String name = '@'+alpha.name.nameClass.toString();
        if( alpha.repeated )    name = name + '+';
        
        out.println(MessageFormat.format(
            "{0} -> {1} [ label=\"{2} {3}\",color=\"{4} 1 .5\",fontcolor=\"{4} 1 .3\" ];",
            new Object[]{ s, state(t.right), name,
                    t.left==null?"":t.left.toString(),
                    new Float(0) } ));
        
        return null;
    }
    
    public Object nonExistentAttribute(NonExistentAttributeAlphabet alpha) {
        String name = alpha.toString();
        
        out.println(MessageFormat.format(
            "{0} -> {1} [ label=\"{2} {3}\",color=\"{4} 1 .5\",fontcolor=\"{4} 1 .3\" ];",
            new Object[]{ s, state(t.right), name,
                    t.left==null?"":t.left.toString(),
                    new Float(0.8) } ));
        
        return null;
    }

    public Object element(ElementAlphabet alpha) {
        out.println(MessageFormat.format(
            "{0} -> {1} [ label=\"{2} {3}\",color=\"{4} 1 .5\",fontcolor=\"{4} 1 .3\" ];",
            new Object[]{ s, state(t.right), alpha.name.nameClass, t.left, new Float(0.2) } ));
        
        return null;
    }

    public Object interleave(InterleaveAlphabet alpha) {
        String tmp = getID();
        out.println(MessageFormat.format(
            "{0} [shape=\"box\",label=\"\",color=\"black\",style=\"filled\"];",
            new Object[]{tmp}));
        out.println(MessageFormat.format(
            "{0} -> {1} [color=\"black\"];",
            new Object[]{s,tmp}));
        out.println(MessageFormat.format(
            "{0} -> {1} [color=\"black\"];",
            new Object[]{tmp,state(t.left)}));
        out.println(MessageFormat.format(
            "{0} -> {1} [color=\"black\"];",
            new Object[]{tmp,state(t.right)}));
        out.println(MessageFormat.format(
            "{0} -> {1} [color=\"black\",style=\"dotted\"];",
            new Object[]{tmp,state(alpha.join)}));
        
        return null;
    }

    public Object list(ListAlphabet alpha) {
        String tmp = getID();
        out.println(tmp+" [shape=\"box\",label=\"\",color=\"purple\",style=\"filled\"];");
        out.println(MessageFormat.format(
            "{0} -> {1} [color=\"purple\"];",
            new Object[]{s,tmp}));
        out.println(MessageFormat.format(
            "{0} -> {1} [color=\"purple\",style=\"dotted\"];",
            new Object[]{tmp,state(t.left)}));
        out.println(MessageFormat.format(
            "{0} -> {1} [color=\"purple\"];",
            new Object[]{tmp,state(t.right)}));
        
        return null;
    }

    public Object data(DataAlphabet alpha) {
        out.println(MessageFormat.format(
            "{0} -> {1} [ label=\"{2} {3}\",color=\"{4} 1 .5\",fontcolor=\"{4} 1 .3\" ];",
            new Object[]{ s, state(t.right), "data", state(t.left), new Float(.4) } ));
        
        return null;
    }

    public Object value(ValueAlphabet alpha) {
        out.println(MessageFormat.format(
            "{0} -> {1} [ label={2},color=\"{3} 1 .5\",fontcolor=\"{3} 1 .3\" ];",
            new Object[]{ s, state(t.right), alpha.value, new Float(.6) } ));
        
        return null;
    }



    /**
     * Sequence number generator. Used when an unique identifier 
     * is necessary.
     */
    private int num =0;
    
    /** Creates a new unique ID. */
    private String getID() {
        return "t"+(num++);
    }
    
}
