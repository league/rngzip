package net.contrapunctus.rngzip;

import java.io.IOException;
import java.util.Map;

public abstract class SingletonState
{
   SingletonState next;

   public void start (SequentialStates st, int elt)
      throws IOException
   {
      die("not expecting <"+elt+">");
   }

   public boolean attrs (SequentialStates st, Map<Integer,String> att)
      throws IOException
   {
      return false;
   }
   
   public void chars (SequentialStates st, char[] buf, int start, int length)
      throws IOException
   {
      /* if we get here, it must be ignorable whitespace. */
      for(int i = start;  i < length;  i++) {
         if(!Character.isWhitespace(buf[i])) {
            die("not expecting PCDATA");
         }
      }
   }
   
   public void end (SequentialStates st)
      throws IOException
   {
      die("not expecting </*>");
   }

   protected final void die(String message)
   {
      throw new IllegalStateException(this+": "+message);
   }

   protected final void dieStart(String expecting, String elt)
   {
      die("expecting "+expecting+", saw <"+elt+">");
   }

   protected final void dieAttr(int a)
   {
      // cannot convert the attr name code to a string from here
      die("unexpected attribute @#"+a);
   }

   protected final void noAttrs(Map<Integer,String> att)
   {
      for(Integer k : att.keySet()) {
         dieAttr(k);
      }
   }
   
   protected final void noAttrsExcept(Map<Integer,String> att, int a1)
   {
      for(Integer k : att.keySet()) {
         if(k == a1) ;
         else dieAttr(k);
      }
   }
   
   protected final void noAttrsExcept(Map<Integer,String> att, int a1, int a2)
   {
      for(Integer k : att.keySet()) {
         if(k == a1) ;
         else if(k == a2) ;
         else dieAttr(k);
      }
   }
}
