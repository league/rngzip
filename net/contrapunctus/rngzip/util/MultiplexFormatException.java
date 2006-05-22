package net.contrapunctus.rngzip.util;

import java.io.IOException;

/** 
 * Signals an error reading a multiplex stream.  Possible errors
 * include a bad magic number, or unexpected end of stream.
 * 
 * <p class='license'>This is free software: you can modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with ABSOLUTELY NO WARRANTY.</p>
 * 
 * @author Copyright ©2005 by
 * <a href="http://contrapunctus.net/league/">Christopher League</a> 
 * @see MultiplexInputStream
 * @see MultiplexOutputStream
 */
public class MultiplexFormatException extends IOException
{
   private MultiplexFormatException(String s)
   {
      super(s);
   }

   private static final long serialVersionUID = 1L;

   static MultiplexFormatException badMagic() throws IOException
   {
      throw new MultiplexFormatException("Bad magic");
   }
   
   static MultiplexFormatException endOfStream(int size)
      throws IOException
   {
      throw new MultiplexFormatException
         ("Premature end of stream: expected block of size "+size);
   }
}
