package net.contrapunctus.rngzip;

import java.util.List;
import java.io.IOException;
import java.io.PrintStream;
import net.contrapunctus.rngzip.io.ChoiceEncoder;
import net.contrapunctus.rngzip.io.RNGZOutputInterface;

class TentativeOutput
   implements RNGZOutputInterface,
              Comparable<TentativeOutput>
{
   private RNGZOutputInterface out;
   private boolean tentative;
   private Event history;

   private static final boolean DEBUG = false;
   private static final PrintStream dbg = System.err;

   public TentativeOutput(RNGZOutputInterface out)
   {
      assert out != null;
      this.out = out;
      this.tentative = false;
      this.history = null;
      assert invariants();
   }

   private TentativeOutput(RNGZOutputInterface out, Event history)
   {
      assert out != null;
      this.out = out;
      this.tentative = true;
      this.history = history;
      assert invariants();
   }

   private boolean invariants()
   {
      assert out != null : "This stream may have already been aborted.";
      assert history == null || tentative : "Non-tentative stream has a history.";
      assert !tentative || history != null : "Tentative stream lacks a history.";
      for(Event e = history;  e != null;  e = e.prev) {
         assert e.next == null : "Part of history has already been committed.";
      }
      return true;
   }

   public ChoiceEncoder makeChoiceEncoder(int limit, Object id)
   {
      return out.makeChoiceEncoder(limit, id);
   }

   public TentativeOutput fork()
   {
      assert invariants();
      tentative = true;
      if(history == null) {
         history = new Sentinel();
      }
      return new TentativeOutput(out, history);
   }

   public void writeChoice(ChoiceEncoder ce, int choice) throws IOException
   {
      assert invariants();
      if(tentative) {
         if(DEBUG) {
            dbg.printf("tentative choice: %s at %s on %s%n", choice, ce, this);
         }
         history = new ChoiceEvent(ce, choice, history);
      }
      else {
         out.writeChoice(ce, choice);
      }
   }
   
   public void writeContent(List<String> path, String s) throws IOException
   {
      assert invariants();
      if(tentative) {
         history = new ContentStrEvent(path, s, history);
      }
      else {
         out.writeContent(path, s);
      }
   }

   public void writeContent(List<String> path, char[] buf, int start, int length)
      throws IOException
   {
      assert invariants();
      if(tentative) {
         history = new ContentBufEvent(path, buf, start, length, history);
      }
      else {
         out.writeContent(path, buf, start, length);
      }
   }

   public void close() throws IOException
   {
      assert !tentative;
      assert invariants();
      out.close();
   }
   
   public void flush() throws IOException
   {
      assert !tentative;
      assert invariants();
      out.flush();
   }

   public int compareTo(TentativeOutput that)
   {
      int this_mag = this.history == null? 0 : this.history.magnitude;
      int that_mag = that.history == null? 0 : that.history.magnitude;
      return this_mag - that_mag;
   }

   private abstract class Event
   {
      private Event prev;
      private Event next = null;
      protected int magnitude = 0;
      private Event(Event prev)
      {
         this.prev = prev;
         if(prev != null) magnitude = prev.magnitude;
      }
      protected abstract void playback(RNGZOutputInterface out) throws IOException;
   }

   private class Sentinel extends Event
   {
      private Sentinel() { super(null); } 
      protected void playback(RNGZOutputInterface out) { }
   }
   
   private class ChoiceEvent extends Event
   {
      private ChoiceEncoder enc;
      private int ch;
      private ChoiceEvent(ChoiceEncoder enc, int ch, Event prev)
      {
         super(prev);
         this.enc = enc;
         this.ch = ch;
         magnitude ++;
      }
      protected void playback(RNGZOutputInterface out) throws IOException
      {
         out.writeChoice(enc, ch);
      }
   }
   
   private class ContentStrEvent extends Event
   {
      private List<String> path;
      private String str;
      private ContentStrEvent(List<String> path, String str, Event prev)
      {
         super(prev);
         this.path = path;
         this.str = str;
         magnitude += str.length();
      }
      protected void playback(RNGZOutputInterface out) throws IOException
      {
         out.writeContent(path, str);
      }
   }

   private class ContentBufEvent extends Event
   {
      private List<String> path;
      private char[] buf;
      private int start, length;
      private ContentBufEvent(List<String> path, char[] buf, 
                           int start, int length, Event prev)
      {
         super(prev);
         this.path = path;
         this.buf = buf;
         this.start = start;
         this.length = length;
         magnitude += length;
      }
      protected void playback(RNGZOutputInterface out) throws IOException
      {
         out.writeContent(path, buf, start, length);
      }
   }
   
   public void commit() throws IOException
   {
      assert invariants();
      if(tentative) {
         /* History is recorded in reverse order, with prev links only.
            Go through and fill in the next links, so we can read it
            forward. */
         Event start = null;
         for(Event e = history;  e != null;  e = e.prev) {
            /* If any of the next pointers are already set, another
               stream must have committed that event already. */
            if(e.prev == null) start = e;
            else e.prev.next = e;
         }
         /* Now we can play it back. */
         for(Event e = start;  e != null;  e = e.next) {
            e.playback(out);
         }
         tentative = false;
         history = null;
         assert invariants();
      }
   }
   
   public void abort() 
   {
      assert tentative;
      tentative = false;
      history = null;
      out = null;
      /* invariant will no longer hold; this object should
         never be used again. */
   }   

//   public static void main(String[] args) throws IOException
//   {
//      TentativeOutput a, b, c, d, e, f;
//      a = new TentativeOutput(new VerboseOutput(System.out));
//
//      a.writeContent(null, "Hello");
//      b = a.fork();
//      a.writeContent(null, "there");
//      b.writeContent(null, "world");
//      c = b.fork();
//      b.writeContent(null, "one");
//      c.writeContent(null, "two");
//      
//      c.abort();
//
//      d = a.fork();
//      e = b.fork();
//      
//      a.writeContent(null, "dar");
//      b.writeContent(null, "kd");
//      d.writeContent(null, "ani");
//      e.writeContent(null, "anne");
//
//      a.commit();
//      
//   }
}
