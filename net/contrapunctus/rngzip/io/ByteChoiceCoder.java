package net.contrapunctus.rngzip.io;

import java.io.IOException;
import net.contrapunctus.rngzip.util.BitOutputStream;
import net.contrapunctus.rngzip.util.BitInputStream;
import net.contrapunctus.rngzip.io.SimpleChoiceCoder;

public class ByteChoiceCoder extends SimpleChoiceCoder
{
      private int trueLimit;

      public ByteChoiceCoder(int limit, Object id)
      {
         super( limit > 256? 65536 : 256, id );
         if( limit > 65536 )
            throw new IllegalArgumentException("limit range");
         trueLimit = limit;
         assert bits % 8 == 0;
      }

      public void encode(int choice, BitOutputStream bo)
         throws IOException
      {
         if( choice >= trueLimit )
            throw new IndexOutOfBoundsException
               ("Choice "+choice+" is out of bounds for choice point "
                +this);
         super.encode( choice, bo );
      }
      
      public int decode(BitInputStream bi)
         throws IOException
      {
         int c = super.decode( bi );
         if( c >= trueLimit )
            throw new RNGZFormatException
               ("input stream produced invalid choice "+c+" at "+this);
         return c;
      }
}
