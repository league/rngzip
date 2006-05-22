package net.contrapunctus.rngzip.util;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.XMLReader;
import com.sun.msv.grammar.Grammar;
import java.io.File;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Stack;
import java.net.URL;
import org.kohsuke.bali.Driver;
import org.kohsuke.bali.automaton.TreeAutomaton;
import org.kohsuke.bali.automaton.State;
import org.kohsuke.bali.automaton.Transition;
import org.kohsuke.bali.automaton.builder.TreeAutomatonBuilder;
import org.kohsuke.bali.optimizer.AttributeReorder;
import org.kohsuke.bali.optimizer.InterleaveStrengthReducer;
import org.kohsuke.bali.optimizer.Unifier;
import org.kohsuke.bali.optimizer.ZeroOrMoreAttributeExpander;
import org.kohsuke.bali.writer.AutomatonDumper;
import org.kohsuke.bali.writer.Interpreter;
import runtime.ValidateletImpl;
import java.net.MalformedURLException;

public class Bali 
{
   public static TreeAutomaton buildAutomatonFromRNG(String file)
      throws MalformedURLException
   {
      Grammar gr = Driver.loadRELAXNGGrammar(fileToURL(file));
      assert gr != null;
      gr = optimize(gr);
      return TreeAutomatonBuilder.build(gr, true, true, true);
   }

   public static void main(String[] args) throws Exception
   {
      TreeAutomaton au = buildAutomatonFromRNG(args[0]);
      System.err.printf("%d states, %d transitions%n",
                        au.countStates(),
                        au.countTransitions());
      System.err.println("** Here is a text dump of the automaton:");
      new AutomatonDumper(System.err).write(au);

      
      //Writer w = new Writer();
      //w.setOutput(System.out, null);
      //      new DebugDecompressor(au).run(w);
      //new DebugDecompressor(au).run(w);
      

//      System.out.println("** Here is the result of encoding/decoding:");
//      new AutomatonDecoder<Object, State>
//         (new DummyAutomatonFactory()).write(au);
      
      /**
      if(args.length > 1) {
         System.out.println("** Creating validator");
         Interpreter in = new Interpreter();
         in.write(au);
         System.setProperty("DEBUG_BALI", "1");
         ValidateletImpl v = in.createValidatelet();
         
         XMLReader xr = XMLReaderFactory.createXMLReader();
         xr.setContentHandler(v);
         xr.parse(args[1]);
         System.out.println("valid");
      }
      else {
         interpret(au.getInitialState());
      }
      **/
   }

   public static PrintStream out = System.err;

   public static void interpret(State s0)
      throws IOException
   {
      BufferedReader rd = new BufferedReader(new InputStreamReader(System.in));
      Stack<State> st = new Stack<State>();
      st.push(s0);

      while(!st.isEmpty()) {
         for(State sj : st) {
            if(sj.isFinal) {
               out.printf("(%s) ", sj);
            }
            else {
               out.printf("%s ", sj);
            }
         }
         State si = st.pop();
         Transition[] ts = si.getTransitions();
         int n = ts.length + (si.isFinal? 1 : 0);
         int i;
         if(n == 0) { 
            out.println(" <pop>");
            // should check if it's okay..
            continue;
         }
         else if(n == 1) {
            out.println(" <continue>");
            i = 0;
         }
         else {                 // n > 1
            out.printf(" <0-%d> ", n-1);
            out.flush();
            String ln = rd.readLine();
            i = Integer.parseInt(ln);
         }
         if(i == ts.length && si.isFinal) { } 
         else {
            out.println(ts[i].alphabet);
            st.push(ts[i].right);
            if(ts[i].left != null) {
               st.push(ts[i].left);
            }
         }
      }
   }

   public static Grammar optimize(Grammar gr)
   {
      gr = Unifier.unify(gr);
      gr = ZeroOrMoreAttributeExpander.optimize(gr);
      gr = InterleaveStrengthReducer.optimize(gr);
      gr = AttributeReorder.optimize(gr);
      return gr;
   }

   public static URL fileToURL(String name)
      throws java.net.MalformedURLException
   {
      return new File(name).toURI().toURL();
   }
}

