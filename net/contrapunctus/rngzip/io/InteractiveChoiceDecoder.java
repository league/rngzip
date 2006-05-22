package net.contrapunctus.rngzip.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import net.contrapunctus.rngzip.util.BitInputStream;
import net.contrapunctus.rngzip.util.BitOutputStream;

class InteractiveChoiceDecoder implements ChoiceDecoder
{
   private BufferedReader in;
   private PrintStream out;
   private Object id;
   private int limit;
   
   public InteractiveChoiceDecoder
      ( BufferedReader in, PrintStream out, 
        int limit, Object id )
   {
      this.in = in;
      this.out = out;
      this.limit = limit;
      this.id = id;
   }
   
   public int decode( BitInputStream br ) throws IOException
   {
      out.printf("%s --%d--> ", this, limit);
      out.flush();
      int ch = Integer.parseInt(in.readLine());
      if(ch < 0 || ch >= limit)
         throw new RNGZFormatException("invalid choice");
      return ch;
   }

   public String toString() 
   {
      if(id == null) return super.toString();
      else return id.toString();
   }
}

