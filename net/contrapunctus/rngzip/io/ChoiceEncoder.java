package net.contrapunctus.rngzip.io;

import java.io.IOException;
import net.contrapunctus.rngzip.util.BitOutputStream;

/**
 * Classes implementing this interface represent choice points in an
 * automaton; they can encode the chosen transition as a sequence of
 * bits.  Different representations of choice points are possible,
 * from a straightforward fixed-length two’s complement representation
 * ({@link SimpleChoiceFactory}) to an adaptive approach that uses
 * shorter bit sequences for more frequently traveled paths ({@link
 * HuffmanChoiceFactory}).  In fact, the {@link VerboseOutput} class
 * employs a ChoiceEncoder that ignores the BitOutputStream and
 * instead writes human-readable information about the choice to a log
 * stream.
 * 
 * <p class='license'>This is free software; you may modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with <b>absolutely no warranty.</b>
 * 
 * @author Christopher League
 * @see ChoiceEncoderFactory
 */
public interface ChoiceEncoder
{
   /**
    * Encodes a representation of ‘choice’ to ‘bo’.  Each instance of
    * ChoiceEncoder represents a particular choice point in an
    * automaton, so it will already know which choices are permitted.
    * 
    * @param choice the choice taken, represented as an integer from
    * zero up to the maximum number of transitions at this choice
    * point.  (The number of choices was provided when this
    * ChoiceEncoder was created; see {@link ChoiceEncoderFactory}.)
    * @param bo the bits are written to this stream.
    * @throws IllegalArgumentException if ‘bo’ is null but this
    * encoder requires an output stream (as most do).
    * @throws IndexOutOfBoundsException if ‘choice’ is out of range.
    * @throws IOException if there is a problem writing to ‘bo’.
    */
   void encode( int choice, BitOutputStream bo ) throws IOException;
}
