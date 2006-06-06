package net.contrapunctus.rngzip;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

class ParallelStates extends CompositeState
{
   private Vector<SequentialStates> vec;
   private IllegalStateException exn;

   ParallelStates(SequentialStates s1, SequentialStates s2)
   {
      vec = new Vector<SequentialStates>(5);
      vec.add(s1);
      vec.add(s2);
   }

   public CompositeState next() throws IOException
   {
      switch(vec.size()) {
      case 0: throw exn;
      case 1: 
         SequentialStates q = vec.get(0);
         q.out.commit();
         q.para = null;
         return q;
      default: return this;
      }
   }

   public CompositeState initialize(SingletonState s0)
   {
      if(vec.size() == 0) throw exn;
      return vec.get(0).initialize(s0);
   }
   
   public CompositeState start(int elt, Map<Integer,String> att)
      throws IOException
   {
      for(Iterator<SequentialStates> i = vec.iterator(); i.hasNext(); ) {
         try {
            i.next().start(elt, att);
         }
         catch(IllegalStateException x) {
            exn = x;
            i.remove();
         }
      }
      return next();
   }

   public CompositeState attrs(Map<Integer,String> att)
      throws IOException
   {
      for(Iterator<SequentialStates> i = vec.iterator(); i.hasNext(); ) {
         try {
            i.next().attrs(att);
         }
         catch(IllegalStateException x) {
            exn = x;
            i.remove();
         }
      }
      return next();
   }

   public CompositeState chars(char[] buf, int start, int length) 
      throws IOException
   {
      for(Iterator<SequentialStates> i = vec.iterator(); i.hasNext(); ) {
         try {
            i.next().chars(buf, start, length);
         }
         catch(IllegalStateException x) {
            exn = x;
            i.remove();
         }
      }
      return next();
   }
   
   public CompositeState end(Map<Integer,String> att) throws IOException
   {
      for(Iterator<SequentialStates> i = vec.iterator(); i.hasNext(); ) {
         try {
            i.next().end(att);
         }
         catch(IllegalStateException x) {
            exn = x;
            i.remove();
         }
      }
      return next();
   }

   public void add(SequentialStates st)
   {
      vec.add(st);
   }

   public void show(PrintWriter pw)
   {
      assert vec.size() > 1;
      pw.print('(');
      boolean first = true;
      for(SequentialStates i : vec) {
         if(first) first = false;
         else pw.print(" | ");
         pw.print(i);
      }
      pw.print(')');
   }     
}
