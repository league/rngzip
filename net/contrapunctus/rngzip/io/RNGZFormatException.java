package net.contrapunctus.rngzip.io;

import java.io.IOException;

/**
 * Signals an error reading from an RNGZInputInterface.  Possible
 * errors include invalid bit representations of choice points.
 * 
 * <p class='license'>This is free software: you can modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with ABSOLUTELY NO WARRANTY.</p>
 * 
 * @author Copyright ©2005 by
 * <a href="http://contrapunctus.net/league/">Christopher League</a> 
 * @see ChoiceDecoder
 */
public class RNGZFormatException extends IOException
{
   RNGZFormatException(String s)
   {
      super(s);
   }
   
   private static final long serialVersionUID = 1L;
}
