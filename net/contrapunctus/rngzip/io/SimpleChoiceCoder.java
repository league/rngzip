package net.contrapunctus.rngzip.io;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import net.contrapunctus.rngzip.util.BitInputStream;
import net.contrapunctus.rngzip.util.BitOutputStream;

/**
 * This class represents choice points using a straightforward
 * fixed-length bit encoding.
 *
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see SimpleChoiceFactory
 */
public class SimpleChoiceCoder 
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

   protected int limit, bits;
   protected Object id;

   /**
    * Create a choice point.  You probably want to construct objects
    * using the {@link SimpleChoiceFactory} instead.
    *
    * @param limit the number of choices at this choice point, which
    * must be <b>greater than 1.</b>
    *
    * @param id this object is just used to represent the choice point
    * for debugging purposes—it may be null.  If non-null, only its
    * ‘toString’ method will be called.
    *
    * @throws AssertionError if ‘limit’ is not greater than 1.
    */
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

   /**
    * Ignore this, it is just used internally to collect statistics
    * about choice points.
    */
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

   public void encode(int choice, BitOutputStream bo)
      throws IOException
   {
      if(bo == null) 
         throw new IllegalArgumentException("bo cannot be null");
      if(choice < 0 || choice >= limit)
         throw new IndexOutOfBoundsException
            ("Choice "+choice+" is out of bounds for choice point "
             +this);
      bo.writeBits(choice, bits);
      if(STATS) tick(choice);
   }

   public int decode(BitInputStream bi) throws IOException
   {
      if(bi == null)
         throw new IllegalArgumentException("bi cannot be null");
      int choice = (int) bi.readBits(bits);
      if(choice < 0 || choice >= limit)
         throw new RNGZFormatException
            ("input stream produced invalid choice "+choice+" at "+this);
      if(STATS) tick(choice);
      return choice;
   }
   
   /**
    * Identifies this choice point using the ‘id’ object provided to
    * the constructor (if it was non-null).
    */
   public String toString() 
   {
      if(id == null) return super.toString();
      else return id.toString();
   }

   /**
    * Ignore this, it is optionally used for gathering statistics
    * about choice points, but the class must be recompiled to support
    * this.
    */
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
