package net.contrapunctus.rngzip.io;

import java.io.PrintStream;
import java.io.IOException;
import net.contrapunctus.rngzip.util.BitOutputStream;

class VerboseChoiceEncoder implements ChoiceEncoder
{
   private PrintStream out;
   private Object id;
   private int limit;
   
   public VerboseChoiceEncoder
      ( PrintStream out, int limit, Object id )
   {
      this.out = out;
      this.limit = limit;
      this.id = id;
   }

   public int magnitude()
   {
      return limit;
   }
   
   public void encode( int choice, BitOutputStream bw ) 
   {
      if(choice < 0 || choice >= limit)
         throw new IndexOutOfBoundsException
            ("Choice "+choice+" is out of bounds for choice point "+this);
      out.printf("choice: %d of %d at %s%n", choice, limit, id);
   }

   public String toString() 
   {
      if(id == null) return super.toString();
      else return id.toString();
   }
}

