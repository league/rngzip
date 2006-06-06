package net.contrapunctus.rngzip;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import net.contrapunctus.rngzip.io.RNGZOutputInterface;
import net.contrapunctus.rngzip.util.ErrorReporter;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class Compressor extends DefaultHandler
{
   protected ErrorReporter err;
   protected Locator loc;
   protected CompositeState state;
   protected Stack<String> elts;
   protected Stack<Map<Integer,String>> atts;
   private static final boolean DEBUG = 
      System.getProperty("DEBUG_Compressor") != null;
   private static final PrintWriter dbg = 
      new PrintWriter(System.err, true);

   public Compressor(ErrorReporter _err, RNGZOutputInterface out)
   {
      err = _err;
      state = new SequentialStates(out);
      elts = new Stack<String>();
      atts = new Stack<Map<Integer,String>>();
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
      atts.clear();
      atts.push(null);
   }

   public void endDocument() throws SAXParseException
   {
      if(DEBUG) { trace("END document"); }
      try { state = state.end(null); }
      catch(IOException exn) { die(exn); }
      catch(IllegalStateException exn) { die(exn); }
   }
   
   public void startElement(String ns, String lname, String qname, 
                            Attributes attr)
      throws SAXParseException
   {
      if(DEBUG) { trace("START "+qname); }
      HashMap<Integer,String> attm = new HashMap<Integer,String>();
      for(int i = 0;  i < attr.getLength();  i++) {
         attm.put(encodeName(attr.getURI(i), attr.getLocalName(i)),
                  attr.getValue(i));
      }
      elts.push(qname);
      atts.push(attm);
      try { state = state.start(encodeName(ns, lname), attm); }
      catch(IOException exn) { die(exn); }
      catch(IllegalStateException exn) { die(exn); }
   }

   public void endElement(String ns, String lname, String qname)
      throws SAXParseException
   {
      if(DEBUG) { trace("END "+qname); }
      assert qname == elts.peek();
      atts.pop();
      try { state = state.end(atts.peek()); }
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


