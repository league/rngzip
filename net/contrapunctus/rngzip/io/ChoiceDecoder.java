package net.contrapunctus.rngzip.io;

import java.io.IOException;
import net.contrapunctus.rngzip.util.BitInputStream;

/**
 * Classes implementing this interface represent choice points in an
 * automaton; they can decode bit sequences to determine which
 * transition to take.
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see ChoiceEncoder
 * @see ChoiceDecoderFactory
 */
public interface ChoiceDecoder
{
   /**
    * Decodes bits from ‘bi’ to determine which choice to take.
    *
    * @param bi the bits are read from this stream.
    * @return the ordinal representation of the choice (zero up to the
    * maximum number of transitions from this choice point).
    * @throws IllegalArgumentException if ‘bi’ is null but this
    * encoder requires an input stream (as most do).
    * @throws RNGZFormatException if the bits on ‘bi’ did not
    * represent a valid transition for this choice point.
    * @throws IOException if there was a problem reading from ‘bi’.
    */
   int decode( BitInputStream bi ) throws IOException;
}
