package net.contrapunctus.rngzip.util;

import com.sun.msv.grammar.Grammar;
import com.sun.msv.util.StringPair;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import org.kohsuke.bali.Driver;
import org.kohsuke.bali.automaton.*;
import org.kohsuke.bali.automaton.builder.TreeAutomatonBuilder;
import org.kohsuke.bali.optimizer.*;
import org.kohsuke.bali.writer.AutomatonWriter;

/**
 * This class encapsulates the <code>TreeAutomaton</code> from the
 * Bali library by Kohsuke Kawaguchi.  It hides some of the details of
 * <code>State</code> and <code>Transition</code> classes, making them
 * easier to use for our application.
 *
 * <p>One of the most important aspects of this class is that it
 * imposes a particular ordering on the transitions based on their
 * alphabets.  This is critical because the compressor and
 * decompressor must agree on the ordering of transitions from each
 * state in the automaton.  The Bali implementation uses maps and
 * sets, which do not maintain reliable transition orderings between
 * different runs.  States are numbered beginning with zero, and
 * transitions leaving each state are numbered similarly.  They
 * include, however, the ‘epsilon’ transitions.
 *
 * <p>For efficiency, Bali maps qualified names to integers.  This
 * class also contains methods <code>encodeName</code> and
 * <code>decodeName</code> to provide convenient access to that
 * mapping.
 *
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see TreeAutomaton
 * @see State
 * @see Transition
 */
public final class BaliAutomaton 
{
   private final URL url;
   private final TreeAutomaton au;
   private Transition[][] trans;
   private State[] states;
   private HashMap<Integer,String> names = new HashMap<Integer,String>();
   private TransitionSorter ts = new TransitionSorter();

   public static BaliAutomaton fromRNG (URL url)
      throws SchemaFormatException
   {
      Grammar gr = Driver.loadRELAXNGGrammar(url);
      if( gr == null ) throw new SchemaFormatException(url.toString());
      gr = Unifier.unify(gr);
      gr = ZeroOrMoreAttributeExpander.optimize(gr);
      gr = InterleaveStrengthReducer.optimize(gr);
      gr = AttributeReorder.optimize(gr);
      TreeAutomaton ta = TreeAutomatonBuilder.build(gr, false, true, true);
      return new BaliAutomaton(url, ta);
   }

   /** 
    * Build an automaton by reading the named Relax NG schema file,
    * and encapsulate it.
    * @param filename path of a RelaxNG schema file (<code>.rng</code>
    * XML format)
    * @throws FileNotFoundException if the specified filename does not
    * exist
    * @throws SchemaFormatException if there is a problem reading a
    * Relax NG schema from the file
    */
   public static BaliAutomaton fromRNG (File file)
      throws FileNotFoundException, SchemaFormatException
   {
      if(!file.exists()) 
         throw new FileNotFoundException(file.toString());
      URL url = null;
      try { url = file.toURI().toURL(); }
      catch(MalformedURLException x) { assert false : x; }
      return fromRNG(url);
   }

   /**
    * Encapsulate the given tree automaton.  This assigns a unique
    * identifier to each state and transition.  The content of the
    * transitions is used to sort them, so that the order remains
    * consistent across multiple runs.
    */
   public BaliAutomaton(URL url, TreeAutomaton au)
   {
      this.url = url;
      this.au = au;
      states = au.getStates();
      /* Build the names map: it is mostly used for debugging. */
      StringBuilder s = new StringBuilder();
      for(StringPair p : au.listNameCodes()) {
         if(p.namespaceURI.length() > 0) {
            s.append(p.namespaceURI);
            s.append(':');
         }
         s.append(p.localName);
         names.put(au.getNameCode(p), s.toString());
         s.setLength(0);
      }
      /* Transitions are stored in a 2-D array. */
      trans = new Transition[states.length][];
      for(int i = 0;  i < states.length;  i++) {
         assert states[i].id == i : states[i];
         trans[i] = states[i].getTransitions();
         /* Now we sort them: the order itself doesn’t really matter,
            but MUST be consistent between different runs. */
         Arrays.sort(trans[i], ts);
      }
   }

   /** 
    * Return the ID of the start state of the automaton.  This usually
    * returns the integer zero, but clients should not depend on that.
    */
   public int initialState()
   {
      return au.getInitialState().id;
   }
   
   /**
    * Return the number of states in this automaton.
    */
   public int countStates() 
   {
      return au.countStates();
   }

   /** 
    * Return the number of transitions from state ‘i’.
    */
   public int countTransitions(int i)
   {
      return trans[i].length;
   }

   /**
    * Return the number of <em>choices</em> from state ‘i’.  This is
    * almost the same as countTransitions, but if ‘i’ is a final
    * state, one more is added.  In a final state with three
    * transitions, you really have four choices: leave by each
    * transition, or stay and terminate.  For non-final states, this
    * is equivalent to countTransitions.
    */
   public int countChoices(int i)
   {
      return trans[i].length + (states[i].isFinal? 1 : 0);
   }

   /**
    * Determine whether state ‘i’ is null.  A null state is a dead
    * end: it’s not a final state, but there are no transitions
    * exiting it either.
    */
   public boolean isNull(int i)
   {
      return (!states[i].isFinal && (trans[i].length == 0));
   }

   /**
    * Determine whether ‘i’ is an <i>epsilon</i> state.  This means
    * that it’s the end of the road (no transitions), but it <i>is</i>
    * a final state.
    */   
   public boolean isEpsilon(int i)
   {
      return states[i].isFinal && (trans[i].length == 0);
   }

   /**
    * Determine whether ‘i’ is a final state.  It may or may not have
    * any transitions.
    */
   public boolean isFinal(int i)
   {
      return states[i].isFinal;
   }
   
   /**
    * This permits clients to explore the alphabet of transition ‘tj’
    * from state ‘si’.  Each transition is described by an
    * <i>alphabet</i> that determines what must be true about the tree
    * in order to take that transition.  There are different kinds of
    * alphabets, related to the element name, presence or absence of
    * particular attributes, character data, constant values, data
    * types, etc.  The way to determine what alphabet is present is to
    * use the <code>AlphabetVisitor</code> interface from the Bali
    * library.
    */
   public Object visitAlphabet(int si, int tj, AlphabetVisitor av)
   {
      return trans[si][tj].alphabet.accept(av);
   }

   /**
    * Returns the child state of the transition ‘tj’ from state ‘si’,
    * or –1 if that transition has no child.  The child state is also
    * referred to as the ‘left’ state, in the interpretation of
    * arbitrary trees as binary trees.
    */
   public int childOf(int si, int tj)
   {
      Transition tr = trans[si][tj];
      if(tr.left == null || isNull(tr.left.id)) return -1;
      else return tr.left.id;
   }

   /**
    * Returns the sibling state, or destination, of the transition
    * ‘tj’ from state ‘si’.  Every transition has a sibling state.  It
    * is also referred to as the ‘right’ child, in the interpretation
    * of arbitrary trees as binary trees.
    */   
   public int siblingOf(int si, int tj)
   {
      return trans[si][tj].right.id;
   }
   
   /**
    * Returns the integer representing the qualified name
    * ‘ns’:‘lname’.  For efficiency, Bali maps qualified names to
    * integers.  This method provides access to the mapping.
    */
   public int encodeName(String ns, String lname)
   {
      return au.getNameCode(ns, lname);
   }

   /**
    * Returns a string representation of the encoded name represented
    * by the integer ‘i’.  For efficiency, Bali maps qualified names
    * to integers.  This method provides the reverse mapping, which is
    * helpful for debugging.
    */
   public String decodeName(int i)
   {
      return names.get(i);
   }

   /**
    * Dumps a text-based representation of the automaton onto the
    * given output stream.  The representation may or may not be
    * sufficient to reconstruct the actualy automaton, but at least it
    * should be enough to <em>distinguish</em> it from other automata.
    * This representation is the basis of the checksum.
    */
   public void print(PrintStream out)
   {
      int n = countStates();
      out.println(n);
      out.println(initialState());
      for(int i = 0;  i < n;  i++)
         {
            int m = countTransitions(i);
            out.println(m);
            int bits = 0;
            if(isEpsilon(i)) bits |= 1;
            if(isFinal(i)) bits |= 2;
            if(isNull(i)) bits |= 4;
            out.println(bits);
            for(int j = 0;  j < m;  j++)
               {
                  out.println(siblingOf(i, j));
                  out.println(visitAlphabet(i, j, ts));
               }
         }
   }

   /** 
    * Compute a checksum of this automaton, using the provided
    * <code>Checksum</code> object.  This works by creating a
    * <code>CheckedOutputStream</code> and calling <code>print</code>
    * to determine the checksum of the printed representation.  The
    * checksum ought to be sufficient to determine—with reasonable
    * probability—that two schemas are the same.
    * @see CheckedOutputStream
    */
   public long checksum(Checksum sum)
   {
      print(new PrintStream
            (new CheckedOutputStream (new NoopOutputStream(), sum)));
      return sum.getValue();
   }

   /**
    * Compute the Adler-32 checksum of this automaton.
    * @see #checksum(Checksum)
    * @see Adler32
    */
   public long checksum()
   {
      return checksum(new Adler32());
   }

   public URL getURL()
   {
      return url;
   }

   /**
    * This shortcut passes the tree automaton to the provided writer
    * from the Bali library.  It can be used to validate an XML stream
    * against the schema, as in the <code>GenericTest</code> program.
    */
   public void writeTo(AutomatonWriter w)
      throws IOException
   {
      w.write(au);
   }
   
   private static final boolean DEBUG = 
      System.getProperty("DEBUG_Automaton") != null;

   /**
    * This program outputs the Adler-32 checksums of all the Relax NG
    * schema files named on the command line.  These checksums can
    * then be embedded in a test suite, to ensure that they do not
    * change over time.  To see the contents of the text stream before
    * the checksum is computed, set <code>-DDEBUG_Automaton</code> on
    * the <code>java</code> command line.
    * @see #checksum()
    */
   public static void main(String[] args) 
      throws FileNotFoundException, SchemaFormatException
   {
      Adler32 sum = new Adler32();
      OutputStream out = DEBUG? System.out : new NoopOutputStream();
      PrintStream pout = new PrintStream(new CheckedOutputStream(out, sum));
      for(String a : args)
         {
            sum.reset();
            BaliAutomaton.fromRNG(new File(a)).print(pout);
            System.out.printf("%08x %s%n", sum.getValue(), a);
         }
   }

   private class TransitionSorter 
      implements AlphabetVisitor, Comparator<Transition>
   {
      public int compare(Transition t1, Transition t2)
      {
         /* first use ID of left branch */
         if(t1.left == null && t2.left != null) return -1;
         if(t1.left != null && t2.left == null) return 1;
         if(t1.left != null) {
            assert t2.left != null;
            if(t1.left.id < t2.left.id) return -1;
            if(t1.left.id > t2.left.id) return 1;
         }
         /* left IDs are equal; break ties with right branch */
         if(t1.right.id < t2.right.id) return -1;
         if(t1.right.id > t2.right.id) return 1;
         /* right IDs are the same; now break ties with the alphabet */
         String s1 = t1.alphabet.accept(this).toString();
         String s2 = t2.alphabet.accept(this).toString();
         return s1.compareTo(s2);
      }
      
      public Object attribute( AttributeAlphabet a ) 
      { 
         return a; 
      }
      public Object nonExistentAttribute( NonExistentAttributeAlphabet a ) 
      {
         return a; 
      }
      public Object element( ElementAlphabet a ) 
      { 
         return a; 
      }
      /* the interleave and list alphabets are not supported yet. */
      public Object interleave( InterleaveAlphabet a ) 
      { 
         assert false; 
         return null; 
      }
      public Object list( ListAlphabet a ) 
      {
         assert false;
         return null; 
      }
      public Object data( DataAlphabet a ) 
      { 
         return "{{DATA}}"; 
      }
      public Object value( ValueAlphabet a ) 
      { 
         return a; 
      }
   }
}

