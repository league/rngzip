package net.contrapunctus.rngzip.io;

import java.io.PrintStream;
import java.io.IOException;
import net.contrapunctus.rngzip.util.BitOutputStream;

/**
 * Objects of this class represent choice points by writing a
 * (somewhat) human-readable trace to an output stream.  It provides a
 * way to see what is going on during the compression process.
 *
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see VerboseOutput
 */
public class VerboseChoiceEncoder implements ChoiceEncoder
{
   private PrintStream out;
   private Object id;
   private int limit;
   
  /**
   * Construct a verbose encoder representing a choice point with up
   * to ‘limit’ possible choices.
   *
   * @param out write the output to this stream.
   * @param limit the number of choices at this choice point, which
   * must be strictly positive.
   * @param id this object is used to represent the choice point for
   * debugging purposes.
   */
   public VerboseChoiceEncoder
      ( PrintStream out, int limit, Object id )
   {
      this.out = out;
      this.limit = limit;
      this.id = id;
   }

  /**
   * Ignores the <code>BitOutputStream</code> and writes instead to
   * the <code>PrintStream</code> given in the constructor.
   */
   public void encode( int choice, BitOutputStream bo ) 
   {
      if(choice < 0 || choice >= limit)
         throw new IndexOutOfBoundsException
            ("Choice "+choice+" is out of bounds for choice point "+this);
      out.printf("choice: %d of %d at %s%n", choice, limit, id);
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

