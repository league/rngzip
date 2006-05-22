package net.contrapunctus.rngzip.util;

import com.sun.msv.util.StringPair;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import org.kohsuke.bali.automaton.*;


/**
 * This class encapsulates the TreeAutomaton from the Bali Validatelet
 * library by Kohsuke Kawaguchi.  It hides some of the details of
 * State and Transition classes, making them easier to use for our
 * application.
 *
 * <p> One of the most important aspects of this class is that it
 * imposes a particular ordering on the transitions based on their
 * alphabets.  This is critical because the compressor and
 * decompressor must agree on the ordering of transitions from each
 * state in the automaton.  The Bali implementation uses maps and
 * sets, which do not maintain reliable transition orderings between
 * different runs.
 *
 * <p>States are numbered beginning with zero, and transitions leaving
 * each state are numbered similarly.  They include, however, the
 * “epsilon” transitions.</p>
 *
 * @author Copyright ©2005 by
 * <a href="http://contrapunctus.net/league/">Christopher League</a> 
 * @see BitInputStream
 */
public final class BaliAutomaton 
{
   private final TreeAutomaton au;
   private Transition[][] trans;
   private State[] states;
   private HashMap<Integer,String> names = new HashMap<Integer,String>();

   /**
    * Massage the given TreeAutomaton and provide an interface for it.
    */
   public BaliAutomaton(TreeAutomaton au)
   {
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
         Arrays.sort(trans[i], new TransitionSorter());
      }
   }

   public BaliAutomaton(String filename) throws MalformedURLException
   {
      this(Bali.buildAutomatonFromRNG(filename));
   }

   /** 
    * Return the ID of the start state of the automaton.
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
    * Return the number of choices from state ‘i’.  This is almost the
    * same as countTransitions, but if ‘i’ is a final state, one more
    * is added.  In a final state with three transitions, you really
    * have four choices: leave by each transition, or stay and
    * terminate.  For non-final states, this is equivalent to
    * countTransitions.
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
    * use the AlphabetVisitor interface from the Bali library.
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

