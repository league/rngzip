package net.contrapunctus.rngzip.io;

import net.contrapunctus.rngzip.util.BitOutputStream;
import net.contrapunctus.rngzip.util.BitInputStream;

final class TrivialChoiceCoder implements ChoiceCoder
{
   static TrivialChoiceCoder instance = new TrivialChoiceCoder();

   public void encode(int choice, BitOutputStream out)
   {
      if(choice != 0) 
         throw new IndexOutOfBoundsException
            ("Choice "+choice+" is out of bounds; 0 required.");
   }
   
   public int decode(BitInputStream in)
   {
      return 0;
   }
}

