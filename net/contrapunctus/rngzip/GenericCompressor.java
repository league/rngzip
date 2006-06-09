package net.contrapunctus.rngzip;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;
import java.util.Map;
import net.contrapunctus.rngzip.io.ChoiceEncoderFactory;
import net.contrapunctus.rngzip.io.ChoiceEncoder;
import net.contrapunctus.rngzip.io.RNGZOutputInterface;
import net.contrapunctus.rngzip.io.RNGZOutputStream;
import net.contrapunctus.rngzip.io.VerboseOutput;
import net.contrapunctus.rngzip.util.BaliAutomaton;
import net.contrapunctus.rngzip.util.ErrorReporter;
import org.kohsuke.bali.automaton.*;
import org.kohsuke.bali.datatype.Value;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

class GenericCompressor extends Compressor
{
   private static final boolean DEBUG = 
      System.getProperty("DEBUG_Compressor") != null;
   private static final PrintStream dbg = System.err;
   private final BaliAutomaton au;
   private ChoiceEncoder[] ces;
   
   public GenericCompressor
      (String filename, ErrorReporter err, RNGZOutputInterface out)
      throws java.net.MalformedURLException
   {
      this(BaliAutomaton.fromRNG(filename), err, out);
   }

   public GenericCompressor
      (BaliAutomaton _au, ErrorReporter err, RNGZOutputInterface out)
   {
      super(err, out);
      au = _au;
      ces = new ChoiceEncoder[au.countStates()];
      for(int i = 0;  i < au.countStates();  i++) {
         int n = au.countChoices(i);
         if(n > 1) {
            ces[i] = out.makeChoiceEncoder(n, i);
         }
      }
   }

   protected SingletonState initialState()
   {
      return new CState(au.initialState());
   }

   public int encodeName(String ns, String lname)
   {
      int r = au.encodeName(ns, lname);
      return r;
   }

   public String decodeName(int e)
   {
      String s = au.decodeName(e);
      return s;
   }   

   private abstract class GenericVisitor implements AlphabetVisitor
   {
      protected StringBuilder expecting = new StringBuilder();
      protected Vector<Integer> matches;
      protected int numTrans, stateID, transID;
      protected GenericVisitor(int id)
      {
         stateID = id;
         numTrans = au.countTransitions(id);
         matches = new Vector<Integer>(numTrans+1);
      }
      protected void run() 
      {
         for(transID = 0;  transID < numTrans;  transID++) {
            if(au.visitAlphabet(stateID, transID, this) == Boolean.TRUE) {
               matches.add(transID);
            }
         }
      }
      protected final void move(SequentialStates st, int si, int tj)
      {
         if(DEBUG) {
            dbg.printf("Visitor: leaving state #%d via transition %d%n", si, tj);
         }
         st.push(makeState(au.siblingOf(si, tj), false));
         int k = au.childOf(si, tj);
         if(k >= 0) {
            st.push(makeState(k, true));
         }
      }
      protected SingletonState makeState(int id, boolean child_p)
      {
         return new CState(id);
      }
      private void move(SequentialStates st, int j)
      {
         if(j < numTrans) {
            move(st, stateID, j);
         }
         else if(DEBUG) {
            dbg.printf("Visotor: leaving state #%d as a FINAL state (transition %d)%n", 
                       stateID, j);
         }
      }
      protected void write(SequentialStates st) throws IOException
      {
      }
      protected boolean exec(SequentialStates st)
         throws IOException
      {
         int n = matches.size();
         if(DEBUG) {
            dbg.printf("GenericVisitor: %d match(es)%n", n);
         }
         if(n == 0) {
            return false;
         }
         st.pop();
         if(ces[stateID] == null) {
            /* this is not a choice point */
            assert n == 1;
            move(st, matches.get(0));
         }
         else if(n == 1) {
            /* this is a choice point, but there was only one match */
            int j = matches.get(0);
            st.writeChoice(ces[stateID], j);
            move(st, j);
         }
         else {
            /* there was more than one match: need to fork. */
            SequentialStates alt = null;
            for(int i = 0;  i < n;  i++) {
               /* EXCEPT for last time through loop: */
               if(i+1 < n) { alt = st.fork(); }
               int j = matches.get(i);
               st.writeChoice(ces[stateID], j);
               move(st, j);
               st = alt;
            }
         }
         write(st);
         return true;
      }
      public Object element(ElementAlphabet a) 
      {
         expecting.append('|');
         expecting.append(a);
         return Boolean.FALSE; 
      }
      public Object attribute( AttributeAlphabet a ) 
      {
         expecting.append('|');
         expecting.append(a);
         return Boolean.FALSE; 
      }
      public Object nonExistentAttribute( NonExistentAttributeAlphabet a ) 
      {
         expecting.append('|');
         expecting.append(a);
         return Boolean.FALSE; 
      }
      public Object interleave( InterleaveAlphabet a ) 
      {
         expecting.append('|');
         expecting.append(a);
         return Boolean.FALSE; 
      }
      public Object list( ListAlphabet a ) 
      {
         expecting.append('|');
         expecting.append(a);
         return Boolean.FALSE; 
      }
      public Object data( DataAlphabet a ) 
      {
         expecting.append('|');
         expecting.append(a);
         return Boolean.FALSE; 
      }
      public Object value( ValueAlphabet a ) 
      {
         expecting.append('|');
         expecting.append(a);
         return Boolean.FALSE; 
      }
   } // end class GenericVisitor

   private class AttributeTracer extends GenericVisitor
   {
      protected Map<Integer,String> atts;
      protected String attr, value, content;
      protected boolean top_p;
      protected AttributeTracer(Map<Integer,String> as, int id, 
                                String at, String val)
      {
         super(id);
         atts = as;
         attr = at;
         value = val;
         top_p = at == null;
         run();
      }
      protected SingletonState makeState(int id, boolean child_p)
      {
         if(DEBUG) {
            dbg.printf("AttributeTracer: making %s state #%d: @%s='%s' [%s]%n",
                       child_p? "CHILD" : "SIBLING", id,
                       attr, value, top_p? "TOP" : "NESTED");
         }
         /* attribute values themselves should not have children, so
            either we're at the top level or this is NOT introducing a
            child state. */
         assert top_p || !child_p;
         if(top_p == child_p) {
            return new CState(id, attr, value);
         }
         else {
            return new CState(id, null, null);
         }
      }
      protected void write(SequentialStates st) throws IOException
      {
         if(content != null) {
            st.writeContent(elts, content);
         }
      }
      public Object attribute(AttributeAlphabet a) 
      {
        if(DEBUG) {
          dbg.printf("AttributeTracer: attr: '%s'%n", a);
        }
         for(Integer k : atts.keySet()) {
            if(a.name.accepts(k)) {
               attr = decodeName(k);
               value = atts.get(k);
               return Boolean.TRUE;
            }
         }
         return super.attribute(a);
      }      
      public Object nonExistentAttribute(NonExistentAttributeAlphabet a)
      {
        if(DEBUG) {
          dbg.printf("AttributeTracer: non-exist attr: '%s'%n", a);
        }
         for(Integer k : atts.keySet()) {
            if(a.accepts(k)) {
               return super.nonExistentAttribute(a);
            }
         }
         return Boolean.TRUE;
      }
      public Object data(DataAlphabet a) 
      {
         if(DEBUG) {
            dbg.printf("AttributeTracer: data value '%s'%n", value);
         }
         if(value == null) {
            return Boolean.FALSE;
         }
         if(value.length() == 0) {
            if(au.isFinal(stateID)) {
               matches.add(numTrans);
            }
            return Boolean.FALSE;
         }
         content = value;
         value = "";
         return Boolean.TRUE;
      }
      public Object value(ValueAlphabet a)
      {
         if(value == null) {
            return Boolean.FALSE;
         }
         Value goal = (Value) a.value;
         if(DEBUG) {
            dbg.printf("AttributeTracer: testing value '%s' alphabet '%s': ", 
                              value, goal.value);
         }
         if(value.equals(goal.value)) {
            if(DEBUG) { dbg.println("yes"); }
            value = "";
            return Boolean.TRUE;
         }
         else {
            if(DEBUG) { dbg.println("no"); }
            return Boolean.FALSE;
         }
      }
   } // end class AttributeTracer

   private class ElementFinder extends GenericVisitor
   {
      protected int elt;
      protected ElementFinder(int _elt, int id)
      {
         super(id);
         elt = _elt;
         run();
      }   
      public Object element( ElementAlphabet a ) 
      {
        boolean p = a.name.accepts(elt);
        if(DEBUG) {
          dbg.printf("ElementFinder: alpha '%s' -> %b%n", a, p);
        }
        if(p) {
            return Boolean.TRUE;
         }
         return super.element(a);
      }
   } // end class ElementFinder

   private class CharMatcher extends GenericVisitor
   {
      protected char[] buf;
      protected int start, length;
      protected CharMatcher(char[] _buf, int _start, int _length, int id)
      {
         super(id);
         buf = _buf;
         start = _start;
         length = _length;
         run();
      }
      protected void write(SequentialStates st) throws IOException
      {
         st.writeContent(elts, buf, start, length);
      }
      public Object data(DataAlphabet a)
      {
         return Boolean.TRUE;
      }
   } // end class CharMatcher
   
   private class CState extends SingletonState
   {
      private int id;
      private String attr, value;
      public String toString() 
      {
        String s = "#"+id;
        if(value != null) {
          s += "@"+attr+((value.length() == 0)? "_" : "~");
        }
        if(next != null) {
          s += ","+next;
        }
        return s;
      }
      private CState(int id) { this.id = id; }
      private CState(int id, String at, String val) { 
         this(id); 
         attr = at;
         value = val; 
      }

      public void start(SequentialStates st, int elt)
         throws IOException
      {
         ElementFinder ef = new ElementFinder(elt, id);
         if(!ef.exec(st)) {
            if(ef.expecting.length() > 0) {
               dieStart(ef.expecting.substring(1), decodeName(elt));
            }
            else {
               die("not expecting <*>, saw <"+decodeName(elt)+">");
            }
         }
      }

      public boolean attrs(SequentialStates st, Map<Integer,String> atts)
         throws IOException
      {
         if(DEBUG) {
            dbg.printf
               ("%s  <> ATTRS (epsilon=%b; final=%b; elts=...%s; atts=", 
                this, au.isEpsilon(id), au.isFinal(id), elts.peek());
            for(int k : atts.keySet()) {
              dbg.printf("@%s ", decodeName(k));
            }
            dbg.printf(")%n");
         }
         if(attr != null && au.isEpsilon(id)) {
            st.pop();
            return true;
            //return false;
         }
         else {
            return new AttributeTracer(atts, id, attr, value).exec(st);
         }
      }
      
      public void chars(SequentialStates st, char[] buf, 
                        int start, int length)
         throws IOException
      {
         new CharMatcher(buf, start, length, id).exec(st);
      }
      
      public void end(SequentialStates st)
         throws IOException
      {
         if(!au.isFinal(id)) super.end(st);
         st.pop();
         int n = au.countChoices(id);
         if(n > 1) {
            st.writeChoice(ces[id], n-1);
         }
      }
   } // end class CState
   
   public static void main(String[] args) throws Exception
   {
      VerboseOutput out = new VerboseOutput(System.err);
      //RNGZOutputInterface out = new RNGZOutputStream
      //   (System.out, RNGZOutputStream.Encoder.HUFFMAN);
      ErrorReporter err = new ErrorReporter();
      GenericCompressor gc = new GenericCompressor(args[0], err, out);
      XMLReader xr = XMLReaderFactory.createXMLReader();
      xr.setContentHandler(gc);
      xr.setErrorHandler(err);
      xr.parse(args[1]);
      out.close();
   }
}


