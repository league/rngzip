package net.contrapunctus.rngzip;

import java.io.IOException;
import java.util.Stack;
import org.kohsuke.bali.automaton.*;
import org.kohsuke.bali.datatype.Value;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import net.contrapunctus.rngzip.util.BaliAutomaton;
import net.contrapunctus.rngzip.util.Bali;
import net.contrapunctus.rngzip.io.ChoiceDecoder;
import net.contrapunctus.rngzip.io.ChoiceDecoderFactory;
import net.contrapunctus.rngzip.io.RNGZInputInterface;
import net.contrapunctus.rngzip.io.RNGZInputStream;
import net.contrapunctus.rngzip.io.InteractiveInput;
import java.net.MalformedURLException;

class GenericDecompressor
   extends Decompressor implements AlphabetVisitor
{
   private final BaliAutomaton au;
   private final ChoiceDecoder[] cds;
   private Stack<Integer> stack = new Stack<Integer>();
   private RNGZInputInterface in;
   private SAXException exn_sax;
   private IOException exn_io;

   public GenericDecompressor(String filename, 
                              RNGZInputInterface in,
                              ContentHandler ch)
      throws MalformedURLException, IOException, SAXException
   {
      this(new BaliAutomaton(filename), in, ch);
   }

   public GenericDecompressor(BaliAutomaton _au, 
                              RNGZInputInterface in,
                              ContentHandler h)
      throws IOException, SAXException
   {
      au = _au;
      cds = new ChoiceDecoder[au.countStates()];
      for(int i = 0;  i < au.countStates();  i++) {
         int n = au.countChoices(i);
         if(n > 1) {
            cds[i] = in.makeChoiceDecoder(n, i);
         }
      }
      run(in, h);
   }

   public void run(RNGZInputInterface in, ContentHandler h)
      throws IOException, SAXException
   {
      initialize(h);
      this.in = in;
      stack.clear();
      stack.push(au.initialState());
      while(!stack.isEmpty()) {
         int state = stack.pop();
         if(au.isNull(state)) continue;
         ChoiceDecoder cd = cds[state];
         int n = au.countChoices(state);
         int i = 0;
         if(cd != null) {
            i = in.readChoice(cd);
            assert i >= 0 && i < n;
         }
         if(au.isFinal(state) && i == n-1) {
            epsilon();
            continue;
         }
         au.visitAlphabet(state, i, this);
         if(exn_io != null) throw exn_io;
         if(exn_sax != null) throw exn_sax;
         stack.push(au.siblingOf(state, i));
         int k = au.childOf(state, i);
         if(k >= 0) {
            stack.push(k);
         }
      }
   }

   public Object attribute (AttributeAlphabet al)
   {
      addAttribute(al.name.nameClass.toString());
      return null;
   }

   public Object nonExistentAttribute (NonExistentAttributeAlphabet al)
   {
      return null;
   }
   
   public Object element (ElementAlphabet al)
   {
      try {
         startElement(al.name.nameClass.toString());
      }
      catch(SAXException e) {
         exn_sax = e;
      }
      return null;
   }
   
   public Object interleave (InterleaveAlphabet al)
   {
      assert false;
      return null;
   }
   
   public Object list (ListAlphabet al)
   {
      assert false;
      return null;
   }
   
   public Object data (DataAlphabet al)
   {
      try { chars(in.readContent(eltStack)); } 
      catch(IOException e) { exn_io = e; }
      catch(SAXException e) { exn_sax = e; }         
      return null;
   }  

   public Object value (ValueAlphabet al)
   {
      //System.err.printf(" *** ValueAlphabet >%s< (%s)%n", al.value,
      //                  al.value.getClass());
      try { 
         Value v = (Value) al.value; // should work?
         chars(v.value.trim());
      }
      catch(SAXException e) { exn_sax = e; }
      return null;
   }
}
