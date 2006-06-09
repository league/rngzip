package net.contrapunctus.rngzip.io;

import net.contrapunctus.rngzip.util.BitOutputStream;
import net.contrapunctus.rngzip.util.BitInputStream;

/**
 * This class represents ‘trivial’ choice points — those where there
 * really is just one way to go.  Thus, this coder never reads or
 * writes any bits to represent its ‘choices’.  This should be used
 * instead of <code>HuffmanChoiceCoder</code> or
 * <code>SimpleChoiceCoder</code> when ‘limit’ is 1.
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 */
public class TrivialChoiceCoder implements ChoiceCoder
{
   /**
    * There is no need to have multiple instances of this class,
    * so refer to this static instance if you need one.
    */
   public static TrivialChoiceCoder instance = new TrivialChoiceCoder();

   private TrivialChoiceCoder()
   {}
   
   public void encode(int choice, BitOutputStream bo)
   {
      if(choice != 0) 
         throw new IndexOutOfBoundsException
            ("Choice "+choice+" is out of bounds; 0 required.");
   }
   
   public int decode(BitInputStream bi)
   {
      return 0;
   }
}
