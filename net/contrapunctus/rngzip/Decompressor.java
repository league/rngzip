package net.contrapunctus.rngzip;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import net.contrapunctus.rngzip.io.RNGZInputInterface;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public abstract class Decompressor
{
   static private abstract class Event {
      abstract void exec (ContentHandler ch) throws SAXException;
      abstract void show (PrintStream out);
      boolean finished () { return true; }
   }

   static private class StartEvent extends Event { 
      String elt;
      AttributesImpl att;
      private boolean finished_p;
      StartEvent(String e) {
         elt = e;
         att = new AttributesImpl();
         finished_p = false;
      }
      void exec (ContentHandler ch) throws SAXException {
         ch.startElement("", "", elt, att);
      }
      void show (PrintStream out) {
         out.print("+" + elt + " ");
         showAttributes(out, att);
         out.println();
      }
      boolean finished() { return finished_p; }
      void commit() { finished_p = true; }
   } // end class StartEvent

   static private class CharEvent extends Event {
      String data;
      CharEvent(String s) {
         data = s;
      }
      void exec (ContentHandler ch) throws SAXException {
         ch.characters(data.toCharArray(), 0, data.length());
      }
      void show (PrintStream out) {
         String s = "$"+data;
         if(s.length() > 12) s = s.substring(0, 9)+"...";
         out.println(s);
      }
   } // end class CharEvent

   static private class EndEvent extends Event {
      String elt;
      EndEvent(String e) {
         elt = e;
      }
      void exec (ContentHandler ch) throws SAXException {
         ch.endElement(null, null, elt);
      }
      void show (PrintStream out) {
         out.println("-" + elt);
      }
   } // end class EndEvent

   private Queue<Event> eventQ = new LinkedList<Event>();
   private Stack<StartEvent> startStack = new Stack<StartEvent>();
   protected Stack<String> eltStack = new Stack<String>();
   private ContentHandler ch;
   private String attrKey;       // current attribute key
   private StringBuilder attrVal = new StringBuilder();

   private static final boolean DEBUG = 
      System.getProperty("DEBUG_Decompressor") != null;
   private static final PrintStream dbg = System.err;

   private void trace (String where)
   {
      dbg.println("===== " + where);
      boolean firstp = true;
      for(StartEvent se : startStack) {
         if(se != null) {
            dbg.printf(" %5s ", firstp? "stack" : "");
            firstp = false;
            se.show(dbg);
         }
      }
      firstp = true;
      for(Event e : eventQ) {
         dbg.printf(" %5s ", firstp? "queue" : "");
         firstp = false;
         e.show(dbg);
      }
   }
   
   protected void initialize (ContentHandler h) throws SAXException
   {
      this.ch = h;
      startStack.push(null);    // sentinel, to detect end of document
      ch.startDocument();
   }

   protected void startElement(String e) throws SAXException
   {
      StartEvent ev = new StartEvent(e);
      startStack.push(ev);
      eltStack.push(e);
      eventQ.add(ev);
      if(DEBUG) trace("startElement");
   }

   protected void addAttribute(String a)
   {
      if(attrKey != null) commitAttribute();
      attrKey = a;
      eltStack.push('@'+a);
      if(DEBUG) trace("addAttribute");
   }

   private void commitAttribute()
   {
      assert attrKey != null;
      eltStack.pop();           // pop "@name"
      startStack.peek().att.addAttribute
         (null, null, attrKey, null, attrVal.toString());
      attrKey = null;
      attrVal.setLength(0);
      if(DEBUG) trace("commitAttribute");
   }

   protected void chars(String s) throws SAXException
   {
      if(attrKey != null) {
         attrVal.append(s);
      }
      else {
         eventQ.add(new CharEvent(s));
      }
   }

   protected void epsilon() throws SAXException
   {
      if(attrKey != null) {
         commitAttribute();
      }
      else {
         endElement();
      }
   }

   private void endElement() throws SAXException
   {
      // TODO: commit corresponding start element
      StartEvent ev = startStack.pop();
      if( ev == null ) {
         endDocument(); // flushes rest of queue
      }
      else {
         ev.commit();
         runQueue();
         String elt = eltStack.pop();
         assert elt.equals(ev.elt);
         eventQ.add(new EndEvent(elt));
         if(DEBUG) trace("endElement");
      }
   }

   private void endDocument() throws SAXException
   {
      runQueue();
      ch.endDocument();
      if(DEBUG) trace("endDocument");
   }

   private void runQueue() throws SAXException
   {
      Event ev = eventQ.peek();
      while( ev != null && ev.finished() ) {
         eventQ.remove();
         ev.exec(ch);
         ev = eventQ.peek();
      }
   }

   public static void showAttributes(PrintStream out, Attributes a)
   {
      out.print("@( ");
      for(int i = 0;  i < a.getLength();  i++) {
         out.printf("%s=\"%s\" ", a.getQName(i), a.getValue(i));
      }
      out.print(") ");
   }
}
