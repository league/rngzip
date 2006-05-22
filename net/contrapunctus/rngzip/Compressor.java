package net.contrapunctus.rngzip;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Stack;
import java.util.HashMap;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import net.contrapunctus.rngzip.util.ErrorReporter;
import net.contrapunctus.rngzip.io.RNGZOutputInterface;

public abstract class Compressor extends DefaultHandler
{
   protected ErrorReporter err;
   protected Locator loc;
   protected CompositeState state;
   protected Stack<String> elts;
   private static final boolean DEBUG = 
      System.getProperty("DEBUG_Compressor") != null;
   private static final PrintWriter dbg = 
      new PrintWriter(System.err, true);

   public Compressor(ErrorReporter _err, RNGZOutputInterface out)
   {
      err = _err;
      state = new SequentialStates(out);
      elts = new Stack<String>();
   }

   protected abstract SingletonState initialState();

   public void setDocumentLocator(Locator _loc)
   {
      loc = _loc;
   }

   protected void die(Exception exn) throws SAXParseException
   {
      err.fatalError(new SAXParseException(exn.getMessage(), loc, exn));
   }

   public abstract int encodeName(String ns, String lname);
   
   public void startDocument() 
   {
      state.initialize(initialState());
      elts.clear();
   }

   public void endDocument() throws SAXParseException
   {
      if(DEBUG) { trace("END document"); }
      try { state = state.end(); }
      catch(IOException exn) { die(exn); }
      catch(IllegalStateException exn) { die(exn); }
   }
   
   public void startElement(String ns, String lname, String qname, 
                            Attributes atts)
      throws SAXParseException
   {
      if(DEBUG) { trace("START "+qname); }
      HashMap<Integer,String> attm = new HashMap<Integer,String>();
      for(int i = 0;  i < atts.getLength();  i++) {
         attm.put(encodeName(atts.getURI(i), atts.getLocalName(i)),
                  atts.getValue(i));
      }
      elts.push(qname);
      try { state = state.start(encodeName(ns, lname), attm); }
      catch(IOException exn) { die(exn); }
      catch(IllegalStateException exn) { die(exn); }
   }

   public void endElement(String ns, String lname, String qname)
      throws SAXParseException
   {
      if(DEBUG) { trace("END "+qname); }
      assert qname == elts.peek();
      try { state = state.end(); }
      catch(IOException exn) { die(exn); }
      catch(IllegalStateException exn) { die(exn); }
      elts.pop();
   }
   
   public void characters(char[] ch, int start, int length)
      throws SAXParseException
   {
      if(DEBUG) { trace("CHARS"); }
      try { state = state.chars(ch, start, length); }
      catch(IOException exn) { die(exn); }
      catch(IllegalStateException exn) { die(exn); }
   }

   protected void trace(String msg) 
   {
      state.show(dbg);
      dbg.print("  <> ");
      dbg.println(msg);
   }
}


