package net.contrapunctus.rngzip;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;
import net.contrapunctus.rngzip.io.ChoiceEncoder;
import net.contrapunctus.rngzip.io.RNGZOutputInterface;

public class SequentialStates extends CompositeState
   implements RNGZOutputInterface
{
   private SingletonState stack;
   ParallelStates para;
   final TentativeOutput out;

   public SequentialStates(RNGZOutputInterface out)
   {
      this(new TentativeOutput(out));
   }

   public SequentialStates(TentativeOutput out)
   {
      this.stack = new FinalState();
      this.out = out;
   }

   private SequentialStates(SingletonState stack,
                            TentativeOutput out)
   {
      this.stack = stack;
      this.out = out;
   }

   public CompositeState initialize(SingletonState s0)
   {
      stack = s0;
      stack.next = null;
      para = null;
      return this;
   }

   public CompositeState start(int elt, Map<Integer,String> att) 
      throws IOException
   {
      stack.start(this, elt);
      return next().attrs(att);
   }

   protected CompositeState attrs(Map<Integer,String> att)
      throws IOException
   {
      if(stack.attrs(this, att)) {
         return next().attrs(att);
      }
      else {
         return next();
      }
   }
   
   public CompositeState chars(char[] buf, int start, int length) 
      throws IOException
   {
      stack.chars(this, buf, start, length);
      return next();
   }
   
   public CompositeState end(Map<Integer,String> att) throws IOException
   {
      stack.end(this);
      if(att != null) {
         return next().attrs(att);
      }
      else {
         return next();
      }
   }

   public void push(SingletonState si)
   {
      si.next = stack;
      stack = si;
   }
   
   public SingletonState pop()
   {
      SingletonState top = stack;
      stack = stack.next;
      top.next = null;
      return top;
   }

   public SequentialStates fork()
   {
      SequentialStates that = new SequentialStates (stack, out.fork());
      if(para == null) {
         para = new ParallelStates(this, that);
      }
      else {
         para.add(that);
      }
      that.para = para;
      return that;
   }

   public CompositeState next()
   {
      if(para == null) return this;
      else return para;
   }
   
   public void show(PrintWriter pw)
   {
      pw.print(stack);
   }

   public ChoiceEncoder makeChoiceEncoder(int limit, Object id)
   {
      return out.makeChoiceEncoder(limit, id);
   }

   public void writeChoice(ChoiceEncoder ce, int choice) throws IOException
   {
      out.writeChoice(ce, choice);
   }
   
   public void writeContent(List<String> path, String dat) throws IOException
   {
      out.writeContent(path, dat);
   }
   
   public void writeContent(List<String> path, char[] buf, int start, int length)
      throws IOException
   {
      out.writeContent(path, buf, start, length);
   }
   
   public void close() throws IOException
   {
      out.close();
   }
   
   public void flush() throws IOException
   {
      out.flush();
   }
   
}
