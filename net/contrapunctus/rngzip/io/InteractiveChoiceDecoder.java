package net.contrapunctus.rngzip.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import net.contrapunctus.rngzip.util.BitInputStream;
import net.contrapunctus.rngzip.util.BitOutputStream;

/**
 * Objects of this class represent choice points by querying the user
 * whenever it needs to make a choice.  This can be useful for tracing
 * and debugging; it provides a way to guide a decompressor
 * interactively through the automaton.
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see InteractiveInput
 */
public class InteractiveChoiceDecoder implements ChoiceDecoder
{
   private BufferedReader in;
   private PrintStream out;
   private Object id;
   private int limit;

   /**
    * Construct an interctive decoder representing a choice point with
    * up to ‘limit’ possible choices.
    *
    * @param in reads user’s input from this stream
    * @param out prompts user on this stream
    * @param limit the number of choices at this choice point, which
    * must be strictly positive.
    * @param id this object is used to represent the choice point for
    * debugging purposes.
    */
   public InteractiveChoiceDecoder
      ( BufferedReader in, PrintStream out, 
        int limit, Object id )
   {
      this.in = in;
      this.out = out;
      this.limit = limit;
      this.id = id;
   }
   

   /**
    * Ignores the <code>BitInputStream</code> and queries the user
    * interactively instead.
    */
   public int decode( BitInputStream bi ) throws IOException
   {
      out.printf("%s --%d--> ", this, limit);
      out.flush();
      int ch = Integer.parseInt(in.readLine());
      if(ch < 0 || ch >= limit)
         throw new RNGZFormatException("invalid choice");
      return ch;
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
}

