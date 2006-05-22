package net.contrapunctus.rngzip.io;

import java.io.IOException;
import java.util.PriorityQueue;
import java.io.PrintStream;
import java.util.Map;
import net.contrapunctus.rngzip.util.BitOutputStream;
import net.contrapunctus.rngzip.util.BitInputStream;
import java.util.HashMap;

final class SimpleChoiceCoder 
   implements ChoiceCoder, Comparable<SimpleChoiceCoder>
{
   private static final boolean STATS = false;
   private static PriorityQueue<SimpleChoiceCoder> collection;
   private HashMap<Integer,Integer> histogram;

   static {
      if(STATS) {
         collection = new PriorityQueue<SimpleChoiceCoder>();
      }
   }

   private int limit, bits;
   private Object id;

   SimpleChoiceCoder(int limit, Object id) 
   {
      assert limit > 1;
      this.limit = limit;
      this.id = id;
      this.bits = (int) Math.ceil(Math.log(limit) / Math.log(2));
      if(STATS) {
         histogram = new HashMap<Integer,Integer>();
         collection.add(this);
      }
   }

   public int compareTo(SimpleChoiceCoder that)
   {
      return that.limit - this.limit;
   }

   private void tick(int choice)
   {
      assert STATS;
      Integer i = histogram.get(choice);
      if(i == null) i = 0;
      histogram.put(choice, i+1);
   }

   public void encode(int choice, BitOutputStream out)
      throws IOException
   {
      if(out == null) 
         throw new IllegalArgumentException("out cannot be null");
      if(choice < 0 || choice >= limit)
         throw new IndexOutOfBoundsException
            ("Choice "+choice+" is out of bounds for choice point "
             +this);
      out.writeBits(choice, bits);
      if(STATS) tick(choice);
   }

   public int decode(BitInputStream in) throws IOException
   {
      if(in == null)
         throw new IllegalArgumentException("in cannot be null");
      int choice = (int) in.readBits(bits);
      if(choice < 0 || choice >= limit)
         throw new RNGZFormatException
            ("input stream produced invalid choice "+choice+" at "+this);
      if(STATS) tick(choice);
      return choice;
   }
   
   public String toString() 
   {
      if(id == null) return super.toString();
      else return id.toString();
   }

   public static void dumpStats(PrintStream out)
   {
      if(!STATS) return;
      while(!collection.isEmpty()) {
         SimpleChoiceCoder cd = collection.remove();
         out.printf("Choice %s : %d%n", cd, cd.limit);
         int n = 0;
         for(Map.Entry<Integer, Integer> e : cd.histogram.entrySet()) {
            if(e.getValue() > 0) {
               out.printf("  %3d chosen %5d times%n", 
                          e.getKey(), e.getValue());
            }
            n += e.getValue();
         }
         out.printf("  (visited %d times)%n", n);
      }
   }
}
