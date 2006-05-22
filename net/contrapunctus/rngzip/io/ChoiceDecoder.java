package net.contrapunctus.rngzip.io;

import java.io.IOException;
import net.contrapunctus.rngzip.util.BitInputStream;

/**
 * Classes implementing this interface represent choice points in an
 * automaton; they can decode bit sequences to determine which
 * transition to take.
 * 
 * <p class='license'>This is free software: you can modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with ABSOLUTELY NO WARRANTY.</p>
 * 
 * @author Copyright ©2005 by
 * <a href="http://contrapunctus.net/league/">Christopher League</a> 
 * @see ChoiceEncoder
 * @see ChoiceDecoderFactory
 */
public interface ChoiceDecoder
{
   /**
    * Decodes bits from ‘in’ to determine which choice to take.
    *
    * @param in The bits are read from this stream.
    * @return The ordinal representation of the choice (zero up to the
    * maximum number of transitions from this choice point).
    * @throws IllegalArgumentException if ‘in’ is null but this
    * encoder requires an input stream (as most do).
    * @throws RNGZFormatException if the bits on ‘in’ did not
    * represent a valid transition for this choice point.
    * @throws IOException if there was a problem reading from ‘in’.
    */
   int decode( BitInputStream in ) throws IOException;
}
