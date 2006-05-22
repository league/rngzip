package net.contrapunctus.rngzip.io;

import java.io.Closeable;
import java.util.List;
import java.io.IOException;

/**
 * This interface specifies the methods needed to read a compressed
 * XML stream. 
 * 
 * <p class='license'>This is free software: you can modify and/or
 * redistribute it under the terms of the GNU General Public License,
 * but it comes with ABSOLUTELY NO WARRANTY.</p>
 * 
 * @author Copyright ©2005 by
 * <a href="http://contrapunctus.net/league/">Christopher League</a> 
 */
public interface RNGZInputInterface extends Closeable, 
                                            ChoiceDecoderFactory
{
   /**
    * Recall which choice to take in the automaton (by reading the
    * input stream).
    * @param dec reperesents the choice point in the automaton; it can
    * decode the choice from a bit stream.
    * @return the ordinal of the choice to take.
    * @throws RNGZFormatException if the input stream does not
    * indicate a valid choice.
    * @throws IOException if there was a problem reading from the
    * input stream.
    */
   int readChoice(ChoiceDecoder dec) throws IOException;

   /**
    * Retrieve a piece of character data from the input stream.  It
    * could be the value of an attribute or some element content.
    * @param path indicates the path from the root to this element in
    * the XML tree.  The last element of this list is the containing
    * element for the characters that are to be read.  Or, if this
    * will be the value of an attribute called “name”, then ‘path’
    * will end with the string “@name”.
    *
    * @throws IllegalArgumentException if ‘path’ is null or an empty
    * list. 
    * @throws IOException if there is a problem writing to the output
    * stream. 
    */
   String readContent(List<String> path) throws IOException;
}
