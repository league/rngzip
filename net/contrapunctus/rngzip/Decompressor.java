package net.contrapunctus.rngzip;

import java.io.PrintStream;
import java.util.Stack;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import java.io.IOException;
import net.contrapunctus.rngzip.io.RNGZInputInterface;

public abstract class Decompressor
{
   protected Stack<String> elts = new Stack<String>();
   private ContentHandler h;
   private boolean attp;      // are we currently accumulating attributes?
   private AttributesImpl atts = new AttributesImpl();
   private String attr;       // current attribute key
   private StringBuilder attv = new StringBuilder();

   private static final boolean DEBUG = 
      System.getProperty("DEBUG_Decompressor") != null;
   private static final PrintStream dbg = System.err;
   
   protected void initialize (ContentHandler h) throws SAXException
   {
      this.h = h;
      elts.push(null);          // used as sentinel to end document
      h.startDocument();
   }

   protected void commitAttribute()
   {
      assert attp;
      if(attr != null) {
         if(DEBUG) {
            dbg.printf("[committing %s==%s]%n", elts.peek(), attr);
         }
         elts.pop();
         atts.addAttribute(null, null, attr, null, attv.toString());
         attr = null;
         attv.delete(0, attv.length());
      }
   }

   protected void commitElement() throws SAXException
   {
      if(attp) {
         commitAttribute();
         if(DEBUG) {
            dbg.printf("[committing <%s>]%n", elts.peek());
         }
         h.startElement("", "", elts.peek(), atts); 
         atts.clear();
         attp = false;
      }
   }

   protected void startElement(String e) throws SAXException
   {
      commitElement();
      elts.push(e);
      attp = true;
   }

   protected void addAttribute(String a)
   {
      assert attp;
      commitAttribute();
      elts.push('@'+a);
      attr = a;
   }

   protected void chars(String s) throws SAXException
   {
      if(attr != null) {
         assert attp;
         attv.append(s);
      }
      else {
         commitElement();
         h.characters(s.toCharArray(), 0, s.length());
      }
   }

   protected void epsilon() throws SAXException
   {
      if(attr != null) {
         commitAttribute();
      }
      else {
         endElement();
      }
   }

   protected void endElement() throws SAXException
   {
      commitElement();
      String e = elts.pop();
      if(e == null) {
         if(DEBUG) {
            dbg.println("[closing document]");
         }
         h.endDocument();
      }
      else {
         if(DEBUG) {
            dbg.printf("[closing </%s>]%n", e);
         }
         h.endElement(null, null, e); 
      }
   }

   protected void endDocument() throws SAXException
   {
      commitElement();
      // Normally this should just do h.endDocument(),
      // but the rest of this cleans up in case we didn't
      // finish properly.
      while(!elts.isEmpty()) {
         endElement();
      }
      h.endDocument(); 
   }
}
